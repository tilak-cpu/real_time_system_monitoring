package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.AgentHeartbeatRequest;
import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.enums.ComputerStatus;
import com.neurosys.backend.repository.ComputerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatTrackerService {

    private final ComputerRepository computerRepository;
    private final WebSocketMetricsPublisher webSocketMetricsPublisher;
    
    // In-memory high performance tracking: Agent ID / Computer ID -> Last Heartbeat Instant
    private final Map<String, Instant> lastHeartbeatMap = new ConcurrentHashMap<>();

    public Map<String, Instant> getLastHeartbeatMap() {
        return lastHeartbeatMap;
    }

    public void updateHeartbeatTime(String computerId) {
        lastHeartbeatMap.put(computerId, Instant.now());
    }

    @Transactional
    public void processHeartbeat(AgentHeartbeatRequest request) {
        if (request.getAgentId() == null || request.getAgentId().isEmpty()) return;

        Optional<Computer> compOpt = computerRepository.findByAgentId(request.getAgentId());
        if (compOpt.isEmpty()) {
            // Auto-heal lookup by hostname fallback
            if (request.getHostname() != null) {
                compOpt = computerRepository.findByHostnameIgnoreCase(request.getHostname());
            }
        }

        if (compOpt.isPresent()) {
            Computer computer = compOpt.get();
            Instant now = Instant.now();
            lastHeartbeatMap.put(computer.getId(), now);
            lastHeartbeatMap.put(computer.getAgentId(), now);

            ComputerStatus oldStatus = computer.getStatus();

            // If computer was OFFLINE, instantly restore status to ONLINE
            if (oldStatus == ComputerStatus.OFFLINE) {
                computer.setStatus(ComputerStatus.ONLINE);
                computer.setLastSeenAt(now);
                computer.setUpdatedAt(now);
                computerRepository.save(computer);

                log.info("[REAL-TIME RESTORE] PC {} ({}) reconnected: OFFLINE → ONLINE", computer.getHostname(), computer.getAgentId());
                webSocketMetricsPublisher.broadcastStatusChange(computer, ComputerStatus.ONLINE, "Connection restored by Agent heartbeat");
            } else {
                // Update lastSeenAt quietly without DB overhead on every ping
                computer.setLastSeenAt(now);
                computerRepository.save(computer);
            }
        }
    }
}
