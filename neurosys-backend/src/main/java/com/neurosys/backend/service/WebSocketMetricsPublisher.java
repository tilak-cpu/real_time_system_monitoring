package com.neurosys.backend.service;

import com.neurosys.backend.dto.response.AlertDto;
import com.neurosys.backend.dto.response.HealthScoreDto;
import com.neurosys.backend.dto.response.SystemMetricDto;
import com.neurosys.backend.entity.Computer;
import com.neurosys.backend.enums.ComputerStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketMetricsPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final List<SseEmitter> sseEmitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerSseClient() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        this.sseEmitters.add(emitter);
        emitter.onCompletion(() -> this.sseEmitters.remove(emitter));
        emitter.onTimeout(() -> this.sseEmitters.remove(emitter));
        emitter.onError((e) -> this.sseEmitters.remove(emitter));
        return emitter;
    }

    public void broadcastStatusChange(Computer computer, ComputerStatus newStatus, String reason) {
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("eventType", "COMPUTER_STATUS_CHANGED");
        eventPayload.put("computerId", computer.getId());
        eventPayload.put("agentId", computer.getAgentId());
        eventPayload.put("hostname", computer.getHostname());
        eventPayload.put("computerName", computer.getComputerName());
        eventPayload.put("status", newStatus.name());
        eventPayload.put("lastSeenAt", Instant.now().toString());
        eventPayload.put("reason", reason != null ? reason : "Heartbeat status update");

        log.info("[REAL-TIME EVENT] COMPUTER_STATUS_CHANGED: {} ({}) → {} [{}]", 
                computer.getHostname(), computer.getAgentId(), newStatus, reason);

        // 1. Broadcast over STOMP WebSocket topics
        messagingTemplate.convertAndSend("/topic/status", eventPayload);
        messagingTemplate.convertAndSend("/topic/dashboard", eventPayload);

        // 2. Broadcast over active SSE Emitters
        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : sseEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("COMPUTER_STATUS_CHANGED")
                        .data(eventPayload));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        sseEmitters.removeAll(deadEmitters);
    }

    public void broadcastTelemetryUpdate(SystemMetricDto metric, HealthScoreDto healthScore, List<AlertDto> newAlerts) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("metric", metric);
        payload.put("healthScore", healthScore);
        payload.put("newAlerts", newAlerts);

        // 1. Broadcast to specific computer channel
        String computerTopic = "/topic/metrics/" + metric.getComputerId();
        messagingTemplate.convertAndSend(computerTopic, payload);

        // 2. Broadcast to main Dashboard overview channel
        messagingTemplate.convertAndSend("/topic/dashboard", payload);

        // 3. Broadcast alerts if triggered
        if (newAlerts != null && !newAlerts.isEmpty()) {
            for (AlertDto alert : newAlerts) {
                messagingTemplate.convertAndSend("/topic/alerts", alert);
            }
        }
    }
}
