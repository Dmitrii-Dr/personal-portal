package com.dmdr.personal.portal.content.dto;

import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.model.MediaEntity;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ArticleMapper {

    public static ArticleResponse toResponse(Article article) {
        if (article == null) {
            return null;
        }

        ArticleResponse response = new ArticleResponse();
        response.setArticleId(article.getArticleId());
        response.setTitle(article.getTitle());
        response.setSlug(article.getSlug());
        response.setContent(article.getContent());
        response.setExcerpt(article.getExcerpt());
        response.setStatus(article.getStatus());
        response.setAuthorId(article.getAuthorId());
        response.setFeaturedImageId(article.getFeaturedImageId());
        response.setCreatedAt(article.getCreatedAt());
        response.setUpdatedAt(article.getUpdatedAt());
        response.setPublishedAt(article.getPublishedAt());

        if (!CollectionUtils.isEmpty(article.getTags())) {
            Set<TagDto> tags = article.getTags().stream()
                    .map(t -> {
                        TagDto dto = new TagDto();
                        dto.setTagId(t.getTagId());
                        dto.setName(t.getName());
                        dto.setSlug(t.getSlug());
                        return dto;
                    })
                    .collect(Collectors.toSet());
            response.setTags(tags);
        }

        if (!CollectionUtils.isEmpty(article.getMediaFiles())) {
            Set<UUID> mediaIds = article.getMediaFiles().stream()
                    .map(MediaEntity::getMediaId)
                    .collect(Collectors.toSet());
            response.setMediaFileIds(mediaIds);
        }

        return response;
    }
}

