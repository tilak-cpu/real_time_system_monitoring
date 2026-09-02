package com.neurosys.backend.scheduler;

import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.enums.ComputerStatus;
import com.neurosys.backend.repository.ComputerRepository;
import com.neurosys.backend.service.AlertEngineService;
import com.neurosys.backend.service.HeartbeatTrackerService;
import com.neurosys.backend.service.WebSocketMetricsPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OfflineDetectionScheduler {

    private final ComputerRepository computerRepository;
    private final AlertEngineService alertEngineService;
    private final HeartbeatTrackerService heartbeatTracker;
    private final WebSocketMetricsPublisher webSocketMetricsPublisher;

    @Scheduled(fixedRate = 1000) // Runs every 1 second for instant ~1-2s offline detection
    @Transactional
    public void detectOfflineComputers() {
        try {
            // 2-second tolerance threshold for network delay
            Instant threshold = Instant.now().minus(2, ChronoUnit.SECONDS);
            Map<String, Instant> heartbeatMap = heartbeatTracker.getLastHeartbeatMap();

            List<Computer> onlineComputers = computerRepository.findAll().stream()
                    .filter(c -> c.getStatus() == ComputerStatus.ONLINE 
                              || c.getStatus() == ComputerStatus.WARNING 
                              || c.getStatus() == ComputerStatus.CRITICAL)
                    .toList();

            for (Computer c : onlineComputers) {
                Instant lastSeen = heartbeatMap.get(c.getId());
                if (lastSeen == null) {
                    lastSeen = heartbeatMap.get(c.getAgentId());
                }
                if (lastSeen == null) {
                    lastSeen = c.getLastSeenAt();
                }

                if (lastSeen == null || lastSeen.isBefore(threshold)) {
                    ComputerStatus oldStatus = c.getStatus();
                    log.info("[REAL-TIME DETECT] PC {} ({}) missed heartbeat (>2s). Status changed {} → OFFLINE", 
                            c.getHostname(), c.getAgentId(), oldStatus);

                    c.setStatus(ComputerStatus.OFFLINE);
                    c.setUpdatedAt(Instant.now());
                    computerRepository.save(c);

                    // Broadcast real-time status change event to all WebSocket & SSE clients
                    webSocketMetricsPublisher.broadcastStatusChange(c, ComputerStatus.OFFLINE, "Connection lost / Heartbeat stopped (>2s)");
                    alertEngineService.triggerOfflineAlert(c);
                }
            }
        } catch (Exception e) {
            log.warn("[OFFLINE SCHEDULER] Transient error during offline check (will retry in next cycle): {}", e.getMessage());
        }
    }
}
