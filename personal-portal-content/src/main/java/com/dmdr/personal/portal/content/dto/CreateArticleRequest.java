package com.dmdr.personal.portal.content.dto;

import com.dmdr.personal.portal.content.model.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CreateArticleRequest extends AbstractArticleRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Slug is required")
    private String slug;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Status is required")
    private ArticleStatus status;

    private Set<UUID> mediaIds;
}

