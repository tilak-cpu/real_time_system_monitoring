package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.SystemMetricsIngestionRequest;
import com.neurosys.backend.dto.response.AlertDto;
import com.neurosys.backend.dto.response.HealthScoreDto;
import com.neurosys.backend.dto.response.SystemMetricDto;
import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.entity.SystemMetric;
import com.neurosys.backend.enums.ComputerStatus;
import com.neurosys.backend.repository.ComputerRepository;
import com.neurosys.backend.repository.SystemMetricRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMetricsServiceImpl implements SystemMetricsService {

    private final SystemMetricRepository systemMetricRepository;
    private final ComputerRepository computerRepository;
    private final HealthScoreEngine healthScoreEngine;
    private final AlertEngineService alertEngineService;
    private final DiagnosisEngineService diagnosisEngineService;
    private final WebSocketMetricsPublisher webSocketMetricsPublisher;
    private final HeartbeatTrackerService heartbeatTracker;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    @Transactional
    public SystemMetricDto ingestMetrics(SystemMetricsIngestionRequest request) {
        Optional<Computer> compOpt = computerRepository.findByAgentId(request.getAgentId());
        
        Computer computer;
        if (compOpt.isPresent()) {
            computer = compOpt.get();
        } else {
            // Auto-heal / Auto-register computer if record missing on server restart
            log.info("[INFO] Auto-registering computer for incoming agent heartbeat: AgentID={}", request.getAgentId());
            computer = Computer.builder()
                    .agentId(request.getAgentId())
                    .hostname("PC-" + request.getAgentId().replaceAll("[^A-Za-z0-9]", ""))
                    .computerName("PC-" + request.getAgentId())
                    .labName("Computer Lab")
                    .status(ComputerStatus.ONLINE)
                    .lastSeenAt(Instant.now())
                    .build();
            computer.setCreatedAt(Instant.now());
            computer.setUpdatedAt(Instant.now());
            computer = computerRepository.save(computer);
            log.info("[INFO] Auto-registered new computer record: ID={}", computer.getId());
        }

        if (computer.getStatus() == ComputerStatus.PENDING || computer.getStatus() == ComputerStatus.REJECTED) {
            log.warn("Blocking metrics ingestion for unapproved computer {} with status {}", computer.getHostname(), computer.getStatus());
            throw new IllegalStateException("Computer endpoint onboarding is " + computer.getStatus() + ". Pending administrator approval.");
        }

        String topProcJson = null;
        if (request.getTopProcesses() != null && !request.getTopProcesses().isEmpty()) {
            try {
                topProcJson = objectMapper.writeValueAsString(request.getTopProcesses());
            } catch (Exception e) {
                log.warn("Failed to serialize top processes", e);
            }
        }

        SystemMetric metric = SystemMetric.builder()
                .computer(computer)
                .cpuUsagePercent(request.getCpuUsagePercent() != null ? request.getCpuUsagePercent() : 0.0)
                .memoryUsagePercent(request.getMemoryUsagePercent() != null ? request.getMemoryUsagePercent() : 0.0)
                .memoryUsedMb(request.getMemoryUsedMb())
                .memoryFreeMb(request.getMemoryFreeMb())
                .diskUsagePercent(request.getDiskUsagePercent() != null ? request.getDiskUsagePercent() : 0.0)
                .diskUsedGb(request.getDiskUsedGb())
                .diskFreeGb(request.getDiskFreeGb())
                .diskReadBytesSec(request.getDiskReadBytesSec())
                .diskWriteBytesSec(request.getDiskWriteBytesSec())
                .networkRxBytesSec(request.getNetworkRxBytesSec())
                .networkTxBytesSec(request.getNetworkTxBytesSec())
                .cpuTemperature(request.getCpuTemperature())
                .activeProcessCount(request.getActiveProcessCount())
                .topProcessesJson(topProcJson)
                .recordedAt(request.getTimestamp() != null ? request.getTimestamp() : Instant.now())
                .build();

        metric.setCreatedAt(Instant.now());
        metric.setUpdatedAt(Instant.now());
        metric = systemMetricRepository.save(metric);

        // Structured Heartbeat & Reconnect Logging
        ComputerStatus oldStatus = computer.getStatus();
        log.info("[INFO] Heartbeat received from {} (Agent: {})", computer.getHostname(), computer.getAgentId());

        Instant now = Instant.now();
        heartbeatTracker.updateHeartbeatTime(computer.getId());
        computer.setLastSeenAt(now);
        if (request.getInternetConnected() != null) {
            computer.setInternetConnected(request.getInternetConnected());
        }
        if (request.getUptimeSeconds() != null) {
            computer.setUptimeSeconds(request.getUptimeSeconds());
        }

        double cpu = request.getCpuUsagePercent() != null ? request.getCpuUsagePercent() : 0.0;
        double ram = request.getMemoryUsagePercent() != null ? request.getMemoryUsagePercent() : 0.0;
        double disk = request.getDiskUsagePercent() != null ? request.getDiskUsagePercent() : 0.0;

        ComputerStatus newStatus;
        if (cpu >= 99.0 || disk >= 99.0) {
            newStatus = ComputerStatus.CRITICAL;
        } else if (cpu >= 90.0 || ram >= 98.0) {
            newStatus = ComputerStatus.WARNING;
        } else {
            newStatus = ComputerStatus.ONLINE;
        }

        if (oldStatus != newStatus) {
            log.info("[INFO] PC {} status restored/changed {} → {}", computer.getHostname(), oldStatus, newStatus);
            webSocketMetricsPublisher.broadcastStatusChange(computer, newStatus, "Telemetry metric ingestion status change");
        }
        computer.setStatus(newStatus);
        computer.setLastSeenAt(now);
        computer.setUpdatedAt(now);
        computerRepository.save(computer);

        // Calculate Health Score
        HealthScoreDto healthScore = healthScoreEngine.calculateAndSaveHealthScore(computer, metric, 0.0);

        // Evaluate Alert Rules
        List<AlertDto> alerts = alertEngineService.evaluateAndTriggerAlerts(computer, metric);

        // Process Diagnosis Incidents & Resolution Detection
        try {
            diagnosisEngineService.processMetricsForIncidents(computer.getId());
        } catch (Exception e) {
            log.warn("Failed processing metrics for diagnosis incidents: {}", e.getMessage());
        }

        SystemMetricDto metricDto = mapToDto(metric);

        // Broadcast to WebSocket clients
        webSocketMetricsPublisher.broadcastTelemetryUpdate(metricDto, healthScore, alerts);

        return metricDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemMetricDto> getMetricHistory(String computerId, int limit) {
        return systemMetricRepository.findByComputerIdOrderByRecordedAtDesc(computerId, PageRequest.of(0, limit))
                .stream().map(this::mapToDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemMetricDto getLatestMetric(String computerId) {
        return systemMetricRepository.findLatestByComputerId(computerId)
                .map(this::mapToDto)
                .orElse(null);
    }

    private SystemMetricDto mapToDto(SystemMetric metric) {
        return SystemMetricDto.builder()
                .id(metric.getId())
                .computerId(metric.getComputer().getId())
                .hostname(metric.getComputer().getHostname())
                .cpuUsagePercent(metric.getCpuUsagePercent())
                .memoryUsagePercent(metric.getMemoryUsagePercent())
                .memoryUsedMb(metric.getMemoryUsedMb())
                .memoryFreeMb(metric.getMemoryFreeMb())
                .diskUsagePercent(metric.getDiskUsagePercent())
                .diskUsedGb(metric.getDiskUsedGb())
                .diskFreeGb(metric.getDiskFreeGb())
                .diskReadBytesSec(metric.getDiskReadBytesSec())
                .diskWriteBytesSec(metric.getDiskWriteBytesSec())
                .networkRxBytesSec(metric.getNetworkRxBytesSec())
                .networkTxBytesSec(metric.getNetworkTxBytesSec())
                .cpuTemperature(metric.getCpuTemperature())
                .activeProcessCount(metric.getActiveProcessCount())
                .recordedAt(metric.getRecordedAt())
                .build();
    }
}
