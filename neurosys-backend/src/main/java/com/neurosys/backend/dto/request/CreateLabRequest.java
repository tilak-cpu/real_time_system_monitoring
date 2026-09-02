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
public class CreateLabRequest {
    @NotBlank(message = "Lab Name is required")
    private String name;

    @NotBlank(message = "Lab Code is required")
    private String code;

    @NotBlank(message = "Location is required")
    private String location;

    private String description;
}
