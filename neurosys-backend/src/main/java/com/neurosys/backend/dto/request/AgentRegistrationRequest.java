package com.neurosys.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRegistrationRequest {

    @NotBlank(message = "Agent ID is required")
    private String agentId;

    @NotBlank(message = "Hostname is required")
    private String hostname;

    private String computerName;

    @NotBlank(message = "IP Address is required")
    private String ipAddress;

    @NotBlank(message = "MAC Address is required")
    private String macAddress;

    @NotBlank(message = "OS Name is required")
    private String osName;

    private String osVersion;
    private String labName;
    private String enrollmentCode;
    private String cpuModel;
    private Double totalRamMb;
    private String agentVersion;
}
