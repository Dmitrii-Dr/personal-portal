package com.dmdr.personal.portal.content.dto;

import com.dmdr.personal.portal.content.model.ArticleStatus;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {

    private UUID articleId;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private ArticleStatus status;
    private UUID authorId;
    private UUID featuredImageId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime publishedAt;
    private Set<TagDto> tags;
    private Set<UUID> mediaFileIds;
}

