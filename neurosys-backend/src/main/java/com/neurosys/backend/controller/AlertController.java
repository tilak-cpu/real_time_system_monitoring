package com.neurosys.backend.controller;

import com.neurosys.backend.dto.response.AlertDto;
import com.neurosys.backend.dto.response.ApiResponse;
import com.neurosys.backend.service.AlertEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Center Endpoint", description = "REST API managing system alerts, severities, acknowledgments, and resolutions")
public class AlertController {

    private final AlertEngineService alertEngineService;

    @GetMapping
    @Operation(summary = "Get Fleet Alerts", description = "Retrieve triggered alerts, optionally filtered by labId")
    public ResponseEntity<ApiResponse<List<AlertDto>>> getAllAlerts(@RequestParam(required = false) String labId) {
        List<AlertDto> alerts = (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId))
                ? alertEngineService.getAlertsByLabId(labId)
                : alertEngineService.getAllAlerts();
        return ResponseEntity.ok(ApiResponse.success("Alerts fetched successfully", alerts));
    }

    @GetMapping("/computer/{computerId}")
    @Operation(summary = "Get Computer Alerts", description = "Retrieve all triggered alerts for a specific computer endpoint")
    public ResponseEntity<ApiResponse<List<AlertDto>>> getComputerAlerts(@PathVariable String computerId) {
        List<AlertDto> alerts = alertEngineService.getComputerAlerts(computerId);
        return ResponseEntity.ok(ApiResponse.success("Alerts fetched successfully", alerts));
    }

    @PutMapping("/{alertId}/acknowledge")
    @Operation(summary = "Acknowledge Alert", description = "Change alert status from OPEN to ACKNOWLEDGED")
    public ResponseEntity<ApiResponse<AlertDto>> acknowledgeAlert(@PathVariable String alertId) {
        AlertDto alert = alertEngineService.acknowledgeAlert(alertId);
        return ResponseEntity.ok(ApiResponse.success("Alert acknowledged successfully", alert));
    }

    @PutMapping("/{alertId}/resolve")
    @Operation(summary = "Resolve Alert", description = "Mark alert status as RESOLVED")
    public ResponseEntity<ApiResponse<AlertDto>> resolveAlert(@PathVariable String alertId) {
        AlertDto alert = alertEngineService.resolveAlert(alertId);
        return ResponseEntity.ok(ApiResponse.success("Alert resolved successfully", alert));
    }
}
