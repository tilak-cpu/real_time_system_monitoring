package com.neurosys.backend.controller;

import com.neurosys.backend.dto.response.ApiResponse;
import com.neurosys.backend.dto.response.ComputerDto;
import com.neurosys.backend.service.ComputerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/computers")
@RequiredArgsConstructor
@Tag(name = "Computers Management Endpoint", description = "REST API retrieving registered computer endpoints, hardware specs, and approval workflows")
public class ComputerController {

    private final ComputerService computerService;

    @GetMapping
    @Operation(summary = "Get All Approved Computers", description = "Retrieve list of all active approved computers, optionally filtered by labId")
    public ResponseEntity<ApiResponse<List<ComputerDto>>> getAllComputers(@RequestParam(required = false) String labId) {
        List<ComputerDto> computers = (labId != null && !labId.isEmpty() && !"ALL".equalsIgnoreCase(labId))
                ? computerService.getComputersByLabId(labId)
                : computerService.getAllComputers();
        return ResponseEntity.ok(ApiResponse.success("Computers fetched successfully", computers));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get Pending Computer Approval Requests", description = "Retrieve list of newly registered computers waiting for administrator approval")
    public ResponseEntity<ApiResponse<List<ComputerDto>>> getPendingComputers() {
        List<ComputerDto> pending = computerService.getPendingComputers();
        return ResponseEntity.ok(ApiResponse.success("Pending computers fetched successfully", pending));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Computer Details by ID", description = "Retrieve detailed system specs, hardware profile, and current status for a computer")
    public ResponseEntity<ApiResponse<ComputerDto>> getComputerById(@PathVariable String id) {
        ComputerDto computer = computerService.getComputerById(id);
        return ResponseEntity.ok(ApiResponse.success("Computer details fetched successfully", computer));
    }

    @GetMapping("/lab/{labName}")
    @Operation(summary = "Get Computers by Lab", description = "Filter computers by computer lab or department name")
    public ResponseEntity<ApiResponse<List<ComputerDto>>> getComputersByLab(@PathVariable String labName) {
        List<ComputerDto> computers = computerService.getComputersByLab(labName);
        return ResponseEntity.ok(ApiResponse.success("Lab computers fetched successfully", computers));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve Computer Endpoint", description = "Approve a pending computer endpoint so it can start sending live telemetry metrics")
    public ResponseEntity<ApiResponse<ComputerDto>> approveComputer(@PathVariable String id) {
        ComputerDto approved = computerService.approveComputer(id);
        return ResponseEntity.ok(ApiResponse.success("Computer endpoint approved successfully", approved));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject Computer Endpoint", description = "Reject a pending computer endpoint to prevent telemetry streaming")
    public ResponseEntity<ApiResponse<ComputerDto>> rejectComputer(@PathVariable String id) {
        ComputerDto rejected = computerService.rejectComputer(id);
        return ResponseEntity.ok(ApiResponse.success("Computer endpoint rejected successfully", rejected));
    }
}
