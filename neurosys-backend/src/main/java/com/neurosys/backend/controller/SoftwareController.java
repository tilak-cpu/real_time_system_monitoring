package com.neurosys.backend.controller;

import com.neurosys.backend.dto.response.ApiResponse;
import com.neurosys.backend.dto.response.LabReadinessDto;
import com.neurosys.backend.dto.response.SoftwareSearchResponse;
import com.neurosys.backend.entity.RequiredSoftware;
import com.neurosys.backend.entity.SoftwareInventory;
import com.neurosys.backend.service.SoftwareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/software")
@RequiredArgsConstructor
@Tag(name = "Software Inventory & Version Checker", description = "REST API for searching software, version checking, and evaluating computer lab software readiness")
public class SoftwareController {

    private final SoftwareService softwareService;

    @GetMapping({"", "/all"})
    @Operation(summary = "Get All Software Inventory", description = "Retrieve list of all applications across all monitored computers in the fleet")
    public ResponseEntity<ApiResponse<List<SoftwareInventory>>> getAllSoftwareInventory() {
        List<SoftwareInventory> list = softwareService.getAllSoftwareInventory();
        return ResponseEntity.ok(ApiResponse.success("All software inventory retrieved", list));
    }

    @GetMapping("/computer/{computerId}")
    @Operation(summary = "Get Installed Software for Computer", description = "Retrieve list of all applications scanned on a specific computer endpoint")
    public ResponseEntity<ApiResponse<List<SoftwareInventory>>> getSoftwareForComputer(@PathVariable String computerId) {
        List<SoftwareInventory> software = softwareService.getSoftwareForComputer(computerId);
        return ResponseEntity.ok(ApiResponse.success("Computer software inventory retrieved", software));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Software Across Fleet", description = "Search software availability (e.g., 'Java 21') across all lab computers showing installed vs missing vs outdated counts")
    public ResponseEntity<ApiResponse<SoftwareSearchResponse>> searchSoftware(
            @RequestParam(required = false, defaultValue = "Java") String query,
            @RequestParam(required = false) String requiredVersion) {
        SoftwareSearchResponse result = softwareService.searchSoftware(query, requiredVersion);
        return ResponseEntity.ok(ApiResponse.success("Software search results retrieved", result));
    }

    @GetMapping("/lab-readiness")
    @Operation(summary = "Check Lab Software Readiness", description = "Evaluate whether computers in a lab meet required software stack criteria for upcoming classes")
    public ResponseEntity<ApiResponse<LabReadinessDto>> getLabReadiness(
            @RequestParam(required = false, defaultValue = "General Lab") String labName,
            @RequestParam(required = false) String labId) {
        LabReadinessDto readiness = softwareService.getLabReadiness(labName, labId);
        return ResponseEntity.ok(ApiResponse.success("Lab readiness evaluated", readiness));
    }

    @GetMapping("/required")
    @Operation(summary = "Get Required Software Requirements", description = "Get list of required software applications configured for a lab")
    public ResponseEntity<ApiResponse<List<RequiredSoftware>>> getRequiredSoftware(
            @RequestParam(required = false, defaultValue = "General Lab") String labName) {
        List<RequiredSoftware> list = softwareService.getRequiredSoftwareForLab(labName);
        return ResponseEntity.ok(ApiResponse.success("Required software rules retrieved", list));
    }

    @PostMapping("/required")
    @Operation(summary = "Add Required Software Requirement", description = "Configure a new required software rule for a lab (e.g. Java 21)")
    public ResponseEntity<ApiResponse<RequiredSoftware>> addRequiredSoftware(
            @RequestParam String labName,
            @RequestParam String softwareName,
            @RequestParam(required = false) String requiredVersion) {
        RequiredSoftware created = softwareService.addRequiredSoftware(labName, softwareName, requiredVersion);
        return ResponseEntity.ok(ApiResponse.success("Required software rule added", created));
    }

    @DeleteMapping("/required/{id}")
    @Operation(summary = "Remove Required Software Requirement", description = "Delete a required software rule by ID")
    public ResponseEntity<ApiResponse<String>> deleteRequiredSoftware(@PathVariable String id) {
        softwareService.removeRequiredSoftware(id);
        return ResponseEntity.ok(ApiResponse.success("Required software rule removed", "OK"));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get Software Inventory Summary", description = "Get distinct application count and last scan timestamp across monitored lab computers")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getSoftwareSummary() {
        java.util.Map<String, Object> summary = softwareService.getSoftwareSummary();
        return ResponseEntity.ok(ApiResponse.success("Software summary retrieved", summary));
    }

    @GetMapping("/fleet-summary")
    @Operation(summary = "Get Atomic Software Fleet Summary & Catalog", description = "Retrieve total computers, total records, distinct packages, computer list, and full software catalog in one atomic snapshot")
    public ResponseEntity<ApiResponse<com.neurosys.backend.dto.response.SoftwareFleetSummaryDto>> getFleetSoftwareSummary() {
        com.neurosys.backend.dto.response.SoftwareFleetSummaryDto summary = softwareService.getFleetSoftwareSummary();
        return ResponseEntity.ok(ApiResponse.success("Fleet software inventory summary retrieved", summary));
    }
}
