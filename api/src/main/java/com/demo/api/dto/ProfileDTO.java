package com.demo.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {

    @Size(min=3,max=30) // When username is not null, length is limited to 3-30
    @Pattern(regexp = "^[A-Za-z0-9_\\-]+$", message = "Username can only contain letters, numbers, underscores, and hyphens")
    private String username; // Username, 3-30 chars, only letters, numbers, underscore and hyphen. Optional

    @Min(0) // When age is not null, the range is 0-150
    @Max(150)
    private Integer age; // Age 0-150. Optional

    @Min(1)
    @Max(2)
    private Integer gender; // Gender, optional. 1: Male, 2: Female

    // Read-only: email in the request body will be ignored, still returned in the response
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String email; // Email

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String avatar; // User avatar.
}
