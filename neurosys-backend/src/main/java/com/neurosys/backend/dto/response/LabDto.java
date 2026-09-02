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
public class LabDto {
    private String id;
    private String name;
    private String code;
    private String location;
    private String description;
    private String status;
    private long totalComputers;
    private long onlineComputers;
    private long offlineComputers;
    private long needsAttentionComputers;
    private Instant createdAt;
}
