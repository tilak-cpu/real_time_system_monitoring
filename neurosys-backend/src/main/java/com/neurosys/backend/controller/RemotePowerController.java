package com.neurosys.backend.controller;

import com.neurosys.backend.dto.request.CommandStatusUpdateRequest;
import com.neurosys.backend.dto.response.ApiResponse;
import com.neurosys.backend.dto.response.RemotePowerAuditDto;
import com.neurosys.backend.dto.response.RemotePowerCommandDto;
import com.neurosys.backend.enums.PowerCommandType;
import com.neurosys.backend.service.RemotePowerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Remote Power Management Endpoint", description = "REST APIs for Administrator remote Lock, Restart, and Shutdown operations")
public class RemotePowerController {

    private final RemotePowerService remotePowerService;

    @PostMapping("/computers/{computerId}/lock")
    @Operation(summary = "Lock Workstation", description = "Lock current Windows session on target computer")
    public ResponseEntity<ApiResponse<RemotePowerCommandDto>> lockComputer(
            @PathVariable String computerId,
            Principal principal) {
        String username = principal != null ? principal.getName() : "Administrator";
        RemotePowerCommandDto command = remotePowerService.issueCommand(computerId, PowerCommandType.LOCK, username);
        return ResponseEntity.ok(ApiResponse.success("Lock command issued successfully", command));
    }

    @PostMapping("/computers/{computerId}/restart")
    @Operation(summary = "Restart Windows", description = "Safely restart target Windows workstation")
    public ResponseEntity<ApiResponse<RemotePowerCommandDto>> restartComputer(
            @PathVariable String computerId,
            Principal principal) {
        String username = principal != null ? principal.getName() : "Administrator";
        RemotePowerCommandDto command = remotePowerService.issueCommand(computerId, PowerCommandType.RESTART, username);
        return ResponseEntity.ok(ApiResponse.success("Restart command issued successfully", command));
    }

    @PostMapping("/computers/{computerId}/shutdown")
    @Operation(summary = "Shut Down Windows", description = "Safely shut down target Windows workstation")
    public ResponseEntity<ApiResponse<RemotePowerCommandDto>> shutdownComputer(
            @PathVariable String computerId,
            Principal principal) {
        String username = principal != null ? principal.getName() : "Administrator";
        RemotePowerCommandDto command = remotePowerService.issueCommand(computerId, PowerCommandType.SHUTDOWN, username);
        return ResponseEntity.ok(ApiResponse.success("Shutdown command issued successfully", command));
    }

    @PostMapping("/computers/bulk-power")
    @Operation(summary = "Bulk Remote Power Action", description = "Execute bulk power command (LOCK, RESTART, SHUTDOWN) scoped to a specific lab or selected workstations")
    public ResponseEntity<ApiResponse<List<RemotePowerCommandDto>>> bulkPowerAction(
            @RequestParam(required = false) String labId,
            @RequestParam(required = false, defaultValue = "SHUTDOWN") PowerCommandType action,
            @RequestBody(required = false) List<String> computerIds,
            Principal principal) {
        String username = principal != null ? principal.getName() : "Administrator";
        List<RemotePowerCommandDto> commands = remotePowerService.issueBulkCommands(labId, computerIds, action, username);
        return ResponseEntity.ok(ApiResponse.success("Bulk power commands issued successfully", commands));
    }

    @GetMapping("/computers/{computerId}/power-audits")
    @Operation(summary = "Get Power Action Audit History", description = "Retrieve audit log history for remote power actions on computer")
    public ResponseEntity<ApiResponse<List<RemotePowerAuditDto>>> getPowerAudits(@PathVariable String computerId) {
        List<RemotePowerAuditDto> audits = remotePowerService.getAuditsForComputer(computerId);
        return ResponseEntity.ok(ApiResponse.success("Power audit history fetched successfully", audits));
    }

    // Agent communication endpoints
    @GetMapping("/agent/power-commands/pending")
    @Operation(summary = "Poll Pending Power Command", description = "Agent polling endpoint to receive pending remote power commands")
    public ResponseEntity<ApiResponse<RemotePowerCommandDto>> pollPendingCommand(@RequestParam String agentId) {
        RemotePowerCommandDto command = remotePowerService.getPendingCommandForAgent(agentId);
        return ResponseEntity.ok(ApiResponse.success(command != null ? "Pending command retrieved" : "No pending commands", command));
    }

    @PostMapping("/agent/power-commands/status")
    @Operation(summary = "Update Power Command Status", description = "Agent reporting endpoint for remote power command status updates")
    public ResponseEntity<ApiResponse<RemotePowerCommandDto>> updateCommandStatus(@RequestBody CommandStatusUpdateRequest request) {
        RemotePowerCommandDto command = remotePowerService.updateCommandStatus(request);
        return ResponseEntity.ok(ApiResponse.success("Command status updated", command));
    }
}
