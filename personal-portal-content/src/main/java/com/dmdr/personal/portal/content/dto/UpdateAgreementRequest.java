package com.dmdr.personal.portal.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateAgreementRequest {

    @NotBlank(message = "Agreement name is required")
    private String name;

    @NotBlank(message = "Agreement content is required")
    private String content;

    @NotBlank(message = "Agreement slug is required")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase letters, numbers, and hyphens (no spaces or special characters)")
    private String slug;
}
