package com.neurosys.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentHeartbeatRequest {
    private String agentId;
    private String hostname;
    private String status;
    private Long uptimeSeconds;
    private Long timestamp;
}
