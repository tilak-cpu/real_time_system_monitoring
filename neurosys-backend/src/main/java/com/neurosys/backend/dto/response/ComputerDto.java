package com.neurosys.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputerDto {
    private String id;
    private String agentId;
    private String hostname;
    private String computerName;
    private String ipAddress;
    private String macAddress;
    private String osName;
    private String osVersion;
    private String labId;
    private String labCode;
    private String labName;
    private String displayName;
    private String cpuModel;
    private Double totalRamMb;
    private String agentVersion;
    private String status;
    private Boolean internetConnected;
    private Long uptimeSeconds;
    private Instant lastSeenAt;
    private Double currentCpuUsage;
    private Double currentRamUsage;
    private Double currentDiskUsage;
    private Double currentHealthScore;
    private Double currentNetworkRxBytesSec;
    private Double currentNetworkTxBytesSec;
    private Double currentNetworkSpeedMbps;
    private Double lastRecordedCpuUsage;
    private Double lastRecordedRamUsage;
    private Double lastRecordedDiskUsage;
    private Instant lastRecordedAt;
}
