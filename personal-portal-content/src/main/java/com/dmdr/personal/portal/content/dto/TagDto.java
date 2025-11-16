package com.dmdr.personal.portal.content.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class TagDto {
    private UUID tagId;

    @NotBlank(message = "Tag name is required")
    private String name;

    @NotBlank(message = "Tag slug is required")
    private String slug;
}


