package com.neurosys.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurosys.backend.dto.response.AlertDto;
import com.neurosys.backend.entity.Alert;
import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.entity.DiagnosticEvent;
import com.neurosys.backend.entity.SystemMetric;
import com.neurosys.backend.enums.AlertSeverity;
import com.neurosys.backend.enums.AlertStatus;
import com.neurosys.backend.enums.AlertType;
import com.neurosys.backend.enums.DiagnosticCategory;
import com.neurosys.backend.exception.ResourceNotFoundException;
import com.neurosys.backend.repository.AlertRepository;
import com.neurosys.backend.repository.DiagnosticEventRepository;
import com.neurosys.backend.repository.SystemMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEngineServiceImpl implements AlertEngineService {

    private final AlertRepository alertRepository;
    private final SystemMetricRepository systemMetricRepository;
    private final DiagnosticEventRepository diagnosticEventRepository;
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;

    private static final List<AlertStatus> ACTIVE_STATUSES = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

    private static final int HISTORY_WINDOW_SIZE = 15;
    private static final double CPU_SUSTAINED_THRESHOLD = 85.0;
    private static final double RAM_SUSTAINED_THRESHOLD = 88.0;
    private static final double DISK_WARNING_THRESHOLD = 85.0;
    private static final double DISK_CRITICAL_THRESHOLD = 92.0;

    @Override
    @Transactional
    public List<AlertDto> evaluateAndTriggerAlerts(Computer computer, SystemMetric metric) {
        List<AlertDto> triggeredAlerts = new ArrayList<>();

        if (computer == null || metric == null) {
            return triggeredAlerts;
        }

        // Auto-resolve offline alert if computer is streaming metrics
        resolveOfflineAlert(computer);

        // Fetch historical telemetry window (up to 15 latest samples)
        List<SystemMetric> history = systemMetricRepository.findByComputerIdOrderByRecordedAtDesc(
                computer.getId(), PageRequest.of(0, HISTORY_WINDOW_SIZE));

        if (history == null || history.size() < 3) {
            // Insufficient historical telemetry to determine degradation pattern — avoid false alerts on single spikes
            log.debug("Insufficient telemetry history ({}) for computer {}. Skipping degradation alert evaluation.",
                    history != null ? history.size() : 0, computer.getHostname());
            return triggeredAlerts;
        }

        int totalSamples = history.size();

        // ----------------------------------------------------
        // 1. PERSISTENT CPU DEGRADATION EVALUATION
        // ----------------------------------------------------
        long highCpuCount = history.stream()
                .filter(m -> m.getCpuUsagePercent() != null && m.getCpuUsagePercent() >= CPU_SUSTAINED_THRESHOLD)
                .count();

        long recentLowCpuCount = history.stream().limit(3)
                .filter(m -> m.getCpuUsagePercent() != null && m.getCpuUsagePercent() < 75.0)
                .count();

        // Requires sustained high CPU in >= 60% of window AND recent samples have NOT recovered to normal
        boolean isCpuPersistent = totalSamples >= 5 && highCpuCount >= (int) (totalSamples * 0.6) && recentLowCpuCount == 0;
        boolean isCpuRecovery = recentLowCpuCount >= 2;

        List<String> cpuEvidence = List.of(
                String.format("CPU usage remained above 85%% in %d out of the last %d telemetry samples.", highCpuCount, totalSamples),
                String.format("Latest recorded CPU load: %.1f%%.", metric.getCpuUsagePercent()),
                "CPU load has remained unusually high for a sustained period compared with normal baseline usage."
        );

        evaluateAlertLifecycle(
                computer,
                AlertType.HIGH_CPU,
                isCpuPersistent,
                isCpuRecovery,
                String.format("%s - Persistent High CPU Usage", computer.getHostname()),
                String.format("%s has shown unusually high CPU usage for a sustained period compared with its normal usage.", computer.getHostname()),
                "Check applications using the most CPU and close unnecessary background tasks.",
                cpuEvidence,
                AlertSeverity.WARNING,
                metric.getCpuUsagePercent(),
                CPU_SUSTAINED_THRESHOLD,
                triggeredAlerts
        );

        // ----------------------------------------------------
        // 2. PERSISTENT MEMORY (RAM) DEGRADATION EVALUATION
        // ----------------------------------------------------
        long highRamCount = history.stream()
                .filter(m -> m.getMemoryUsagePercent() != null && m.getMemoryUsagePercent() >= RAM_SUSTAINED_THRESHOLD)
                .count();

        long recentLowRamCount = history.stream().limit(3)
                .filter(m -> m.getMemoryUsagePercent() != null && m.getMemoryUsagePercent() < 80.0)
                .count();

        boolean isRamPersistent = totalSamples >= 5 && highRamCount >= (int) (totalSamples * 0.6) && recentLowRamCount == 0;
        boolean isRamRecovery = recentLowRamCount >= 2;

        List<String> ramEvidence = List.of(
                String.format("RAM allocation remained above 88%% in %d out of the last %d telemetry samples.", highRamCount, totalSamples),
                String.format("Latest recorded RAM allocation: %.1f%% (%.0f MB free).", metric.getMemoryUsagePercent(), metric.getMemoryFreeMb() != null ? metric.getMemoryFreeMb() : 0.0),
                "Memory usage has remained continuously high and available RAM has not recovered."
        );

        evaluateAlertLifecycle(
                computer,
                AlertType.HIGH_RAM,
                isRamPersistent,
                isRamRecovery,
                String.format("%s - Persistent Memory Pressure", computer.getHostname()),
                String.format("Memory allocation on %s has remained continuously high for a sustained period.", computer.getHostname()),
                "Close memory-intensive applications or restart background services.",
                ramEvidence,
                AlertSeverity.WARNING,
                metric.getMemoryUsagePercent(),
                RAM_SUSTAINED_THRESHOLD,
                triggeredAlerts
        );

        // ----------------------------------------------------
        // 3. STORAGE DEGRADATION & CONSUMPTION HORIZON PREDICTION
        // ----------------------------------------------------
        double diskPercent = metric.getDiskUsagePercent() != null ? metric.getDiskUsagePercent() : 0.0;
        double freeDiskGb = metric.getDiskUsedGb() != null && metric.getDiskFreeGb() != null ? metric.getDiskFreeGb() : 100.0;

        boolean isDiskWarning = diskPercent >= DISK_WARNING_THRESHOLD || freeDiskGb <= 15.0;
        boolean isDiskCritical = diskPercent >= DISK_CRITICAL_THRESHOLD || freeDiskGb <= 8.0;
        boolean isDiskRecovery = diskPercent < 80.0 && freeDiskGb > 20.0;

        // Calculate rate of consumption for storage prediction
        double oldestDiskFreeGb = history.get(history.size() - 1).getDiskFreeGb() != null ? history.get(history.size() - 1).getDiskFreeGb() : freeDiskGb;
        double diskBurnGb = oldestDiskFreeGb - freeDiskGb;
        int estimatedDays = diskBurnGb > 0.5 ? Math.max(1, (int)(freeDiskGb / diskBurnGb * 3.0)) : 14;

        String diskMsg = isDiskCritical
                ? String.format("Storage space is running critically low (%.1f GB remaining). At current consumption rate, storage may run out in ~%d days.", freeDiskGb, estimatedDays)
                : String.format("Free storage space is running low (%.1f%% used).", diskPercent);

        List<String> diskEvidence = List.of(
                String.format("Storage utilization is currently %.1f%%.", diskPercent),
                String.format("Free storage space remaining: %.1f GB.", freeDiskGb),
                String.format("Calculated storage exhaustion horizon: ~%d days.", estimatedDays)
        );

        evaluateAlertLifecycle(
                computer,
                AlertType.HIGH_DISK,
                isDiskWarning || isDiskCritical,
                isDiskRecovery,
                isDiskCritical ? String.format("%s - CRITICAL STORAGE LOW", computer.getHostname()) : String.format("%s - Storage Space Running Low", computer.getHostname()),
                diskMsg,
                "Clean up temporary files, clear system caches, and uninstall unused applications.",
                diskEvidence,
                isDiskCritical ? AlertSeverity.CRITICAL : AlertSeverity.WARNING,
                diskPercent,
                isDiskCritical ? DISK_CRITICAL_THRESHOLD : DISK_WARNING_THRESHOLD,
                triggeredAlerts
        );

        // ----------------------------------------------------
        // 4. MULTI-SIGNAL URGENT SYSTEM DEGRADATION EVALUATION
        // ----------------------------------------------------
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<DiagnosticEvent> recentCrashes = diagnosticEventRepository.findByComputerIdOrderByOccurredAtDesc(
                computer.getId(), PageRequest.of(0, 10)).stream()
                .filter(e -> e.getOccurredAt().isAfter(sevenDaysAgo) && 
                        (e.getCategory() == DiagnosticCategory.GRAPHICS || e.getCategory() == DiagnosticCategory.UNEXPECTED_SHUTDOWN || e.getCategory() == DiagnosticCategory.SYSTEM_CRASH))
                .toList();

        boolean isHighTemp = metric.getCpuTemperature() != null && metric.getCpuTemperature() >= 82.0;
        int activeFailureFactors = 0;
        if (isCpuPersistent) activeFailureFactors++;
        if (isRamPersistent) activeFailureFactors++;
        if (isHighTemp) activeFailureFactors++;
        if (!recentCrashes.isEmpty()) activeFailureFactors += recentCrashes.size();

        boolean isUrgentDegradation = activeFailureFactors >= 3;
        boolean isUrgentResolved = activeFailureFactors < 2;

        List<String> riskEvidence = List.of(
                String.format("CPU usage sustained above 85%% (%s).", isCpuPersistent ? "YES" : "NO"),
                String.format("RAM allocation sustained above 88%% (%s).", isRamPersistent ? "YES" : "NO"),
                String.format("System/application crashes in last 7 days: %d events.", recentCrashes.size()),
                String.format("Processor thermal workload: %s.", isHighTemp ? String.format("%.1f°C", metric.getCpuTemperature()) : "Normal")
        );

        evaluateAlertLifecycle(
                computer,
                AlertType.HIGH_RISK,
                isUrgentDegradation,
                isUrgentResolved,
                String.format("%s - URGENT SYSTEM INSTABILITY RISK", computer.getHostname()),
                String.format("%s is showing multiple signs of performance degradation and repeated system errors. The computer may become unstable.", computer.getHostname()),
                "Inspect hardware cooling, test RAM memory integrity, and review recorded Windows system logs.",
                riskEvidence,
                AlertSeverity.CRITICAL,
                (double) activeFailureFactors,
                3.0,
                triggeredAlerts
        );

        return triggeredAlerts;
    }

    @Override
    @Transactional
    public void triggerOfflineAlert(Computer computer) {
        if (computer == null) return;
        List<AlertDto> dummyList = new ArrayList<>();
        evaluateAlertLifecycle(
                computer,
                AlertType.OFFLINE,
                true,
                false,
                String.format("%s Endpoint Offline", computer.getHostname()),
                String.format("%s missed telemetry heartbeat (>60s) and is currently offline.", computer.getHostname()),
                "Check computer power supply and physical network connection.",
                List.of("No telemetry heartbeat received for >60 seconds.", "Computer marked OFFLINE in system inventory."),
                AlertSeverity.WARNING,
                0.0,
                1.0,
                dummyList
        );
    }

    @Override
    @Transactional
    public void resolveOfflineAlert(Computer computer) {
        if (computer == null) return;
        List<AlertDto> dummyList = new ArrayList<>();
        evaluateAlertLifecycle(
                computer,
                AlertType.OFFLINE,
                false,
                true,
                "Computer Endpoint Offline",
                "",
                "",
                List.of(),
                AlertSeverity.WARNING,
                0.0,
                1.0,
                dummyList
        );
    }

    private void evaluateAlertLifecycle(
            Computer computer,
            AlertType alertType,
            boolean isPersistentCondition,
            boolean isRecoveryCondition,
            String title,
            String message,
            String recommendedAction,
            List<String> evidenceList,
            AlertSeverity severity,
            Double triggeredValue,
            Double thresholdValue,
            List<AlertDto> triggeredAlerts
    ) {
        Optional<Alert> activeAlert = alertRepository.findFirstByComputerIdAndAlertTypeAndStatusIn(
                computer.getId(), alertType, ACTIVE_STATUSES
        );

        // Check if administrator manually resolved an alert for this computer & alertType within the last 15 minutes
        Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);
        boolean recentlyResolvedByAdmin = alertRepository.existsByComputerIdAndAlertTypeAndStatusAndResolvedAtAfter(
                computer.getId(), alertType, AlertStatus.RESOLVED, fifteenMinutesAgo
        );

        String evidenceJson = null;
        if (evidenceList != null && !evidenceList.isEmpty()) {
            try {
                evidenceJson = objectMapper.writeValueAsString(evidenceList);
            } catch (Exception e) {
                evidenceJson = "[]";
            }
        }

        if (isPersistentCondition) {
            if (activeAlert.isEmpty()) {
                if (recentlyResolvedByAdmin) {
                    // Admin manually resolved this incident recently -> Respect admin resolution & snooze re-triggering!
                    log.debug("Alert {} for {} was recently resolved by admin. Respecting resolution.", alertType, computer.getHostname());
                    return;
                }

                // Persistent condition confirmed -> Create ONE active incident alert
                Alert alert = Alert.builder()
                        .computer(computer)
                        .title(title)
                        .message(message)
                        .recommendedAction(recommendedAction)
                        .evidenceJson(evidenceJson)
                        .severity(severity)
                        .alertType(alertType)
                        .status(AlertStatus.OPEN)
                        .triggeredValue(triggeredValue)
                        .thresholdValue(thresholdValue)
                        .occurrenceCount(1)
                        .firstDetectedAt(Instant.now())
                        .lastDetectedAt(Instant.now())
                        .triggeredAt(Instant.now())
                        .build();

                alert = alertRepository.save(alert);
                log.info("[INFO] Persistent Alert Triggered [Type: {}, Computer: {}]: {}", alertType, computer.getHostname(), title);

                emailNotificationService.sendCriticalAlertEmail(alert);
                triggeredAlerts.add(mapToDto(alert));
            } else {
                // Problem remains ACTIVE -> Update existing active incident (DEDUPLICATION GUARANTEE)
                Alert existing = activeAlert.get();
                existing.setOccurrenceCount((existing.getOccurrenceCount() != null ? existing.getOccurrenceCount() : 1) + 1);
                existing.setLastDetectedAt(Instant.now());
                existing.setTriggeredValue(triggeredValue);
                if (evidenceJson != null) existing.setEvidenceJson(evidenceJson);
                existing.setSeverity(severity);
                
                alertRepository.save(existing);
                log.debug("[INFO] Updated active incident [Type: {}, Computer: {}] (Occurrences: {})",
                        alertType, computer.getHostname(), existing.getOccurrenceCount());
            }
        } else if (isRecoveryCondition) {
            if (activeAlert.isPresent()) {
                // Condition Recovered -> Automatically resolve existing active incident
                Alert alertToResolve = activeAlert.get();
                alertToResolve.setStatus(AlertStatus.RESOLVED);
                alertToResolve.setResolvedAt(Instant.now());
                if (!alertToResolve.getMessage().contains("returned to normal")) {
                    alertToResolve.setMessage(alertToResolve.getMessage() + " (Condition returned to normal)");
                }
                alertRepository.save(alertToResolve);

                log.info("[INFO] Alert Condition Recovered. Resolved incident [Type: {}, Computer: {}]",
                        alertType, computer.getHostname());

                emailNotificationService.sendAlertRecoveryEmail(alertToResolve);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAllAlerts() {
        return alertRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getAlertsByLabId(String labId) {
        if (labId == null || labId.isEmpty() || "ALL".equalsIgnoreCase(labId)) {
            return getAllAlerts();
        }
        return alertRepository.findAll().stream()
                .filter(a -> a.getComputer() != null && a.getComputer().getLab() != null && labId.equals(a.getComputer().getLab().getId()))
                .map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertDto> getComputerAlerts(String computerId) {
        return alertRepository.findByComputerIdOrderByTriggeredAtDesc(computerId)
                .stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional
    public AlertDto acknowledgeAlert(String alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
        alert.setStatus(AlertStatus.ACKNOWLEDGED);
        alert = alertRepository.save(alert);
        return mapToDto(alert);
    }

    @Override
    @Transactional
    public AlertDto resolveAlert(String alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", alertId));
        alert.setStatus(AlertStatus.RESOLVED);
        alert.setResolvedAt(Instant.now());
        alert = alertRepository.save(alert);
        log.info("[INFO] Manually resolved alert {} for computer {}", alertId, alert.getComputer().getHostname());
        return mapToDto(alert);
    }

    private AlertDto mapToDto(Alert alert) {
        List<String> evidence = new ArrayList<>();
        if (alert.getEvidenceJson() != null && !alert.getEvidenceJson().isEmpty()) {
            try {
                evidence = objectMapper.readValue(alert.getEvidenceJson(), List.class);
            } catch (Exception ignored) {
            }
        }

        Computer comp = alert.getComputer();
        String cName = comp != null && comp.getDisplayName() != null && !comp.getDisplayName().isEmpty() ? comp.getDisplayName() : (comp != null ? comp.getHostname() : "");
        String lId = comp != null && comp.getLab() != null ? comp.getLab().getId() : "";
        String lCode = comp != null && comp.getLab() != null ? comp.getLab().getCode() : "LAB";
        String lName = comp != null && comp.getLab() != null ? comp.getLab().getName() : (comp != null && comp.getLabName() != null ? comp.getLabName() : "Computer Lab 1");

        return AlertDto.builder()
                .id(alert.getId())
                .computerId(comp != null ? comp.getId() : "")
                .hostname(comp != null ? comp.getHostname() : "")
                .computerName(cName)
                .labId(lId)
                .labCode(lCode)
                .labName(lName)
                .title(alert.getTitle())
                .message(alert.getMessage())
                .recommendedAction(alert.getRecommendedAction())
                .evidence(evidence)
                .severity(alert.getSeverity().name())
                .alertType(alert.getAlertType().name())
                .status(alert.getStatus().name())
                .triggeredValue(alert.getTriggeredValue())
                .thresholdValue(alert.getThresholdValue())
                .occurrenceCount(alert.getOccurrenceCount() != null ? alert.getOccurrenceCount() : 1)
                .firstDetectedAt(alert.getFirstDetectedAt() != null ? alert.getFirstDetectedAt() : alert.getTriggeredAt())
                .lastDetectedAt(alert.getLastDetectedAt() != null ? alert.getLastDetectedAt() : alert.getTriggeredAt())
                .triggeredAt(alert.getTriggeredAt())
                .resolvedAt(alert.getResolvedAt())
                .build();
    }
}
