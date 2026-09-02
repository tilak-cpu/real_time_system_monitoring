package com.neurosys.backend.service;

import com.neurosys.backend.dto.request.SoftwareItemDto;
import com.neurosys.backend.dto.request.SoftwareSyncRequest;
import com.neurosys.backend.dto.response.LabReadinessDto;
import com.neurosys.backend.dto.response.SoftwareFleetSummaryDto;
import com.neurosys.backend.dto.response.SoftwareSearchResponse;
import com.neurosys.backend.entity.RequiredSoftware;
import com.neurosys.backend.entity.SoftwareInventory;

import java.util.List;

public interface SoftwareService {

    void syncSoftwareInventory(SoftwareSyncRequest request);

    List<SoftwareInventory> getAllSoftwareInventory();

    List<SoftwareInventory> getSoftwareForComputer(String computerId);

    SoftwareSearchResponse searchSoftware(String query, String requiredVersion);

    LabReadinessDto getLabReadiness(String labName);

    LabReadinessDto getLabReadiness(String labName, String labId);

    RequiredSoftware addRequiredSoftware(String labName, String softwareName, String requiredVersion);

    List<RequiredSoftware> getRequiredSoftwareForLab(String labName);

    void removeRequiredSoftware(String id);

    java.util.Map<String, Object> getSoftwareSummary();

    SoftwareFleetSummaryDto getFleetSoftwareSummary();
}
