package com.neurosys.backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateEnrollmentCodeRequest {
    private Integer expirationHours;
    private Integer maxUses;
}
