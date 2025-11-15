package com.dmdr.personal.portal.content.dto;

import com.dmdr.personal.portal.content.model.ArticleStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractArticleRequest {

    private String title;

    private String slug;

    private String content;

    private String excerpt;

    private ArticleStatus status;

    private UUID featuredImageId;

    private Set<UUID> tagIds;

    private Set<UUID> mediaFileIds;

    private Set<UUID> allowedUserIds;
}

