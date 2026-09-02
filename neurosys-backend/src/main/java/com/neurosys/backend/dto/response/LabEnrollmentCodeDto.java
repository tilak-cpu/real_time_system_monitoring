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
public class LabEnrollmentCodeDto {
    private String id;
    private String code;
    private String labId;
    private String labName;
    private String createdBy;
    private Instant expiresAt;
    private Integer maxUses;
    private Integer currentUses;
    private Boolean revoked;
    private Instant createdAt;
}
