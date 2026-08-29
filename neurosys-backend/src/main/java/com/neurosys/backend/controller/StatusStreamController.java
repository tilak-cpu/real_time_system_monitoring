package com.neurosys.backend.controller;

import com.neurosys.backend.service.WebSocketMetricsPublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Real-Time Event Stream Endpoint", description = "Server-Sent Events (SSE) streaming for real-time status changes")
public class StatusStreamController {

    private final WebSocketMetricsPublisher metricsPublisher;

    @GetMapping(value = "/status-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to Real-Time Status Stream", description = "Streams COMPUTER_STATUS_CHANGED events instantly via SSE")
    public SseEmitter subscribeStatusStream() {
        return metricsPublisher.registerSseClient();
    }
}
