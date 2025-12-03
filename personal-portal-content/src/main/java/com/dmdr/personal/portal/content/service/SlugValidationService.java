package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.repository.ArticleRepository;
import org.springframework.stereotype.Service;

@Service
public class SlugValidationService {

    private final ArticleRepository articleRepository;

    public SlugValidationService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
    }

    /**
     * Validates that a slug is unique in Articles.
     * 
     * @param slug the slug to validate
     * @param excludeArticleId optional article ID to exclude from check (for updates)
     * @throws IllegalArgumentException if the slug already exists
     */
    public void validateSlugUniqueness(String slug, java.util.UUID excludeArticleId) {
        if (slug == null || slug.isEmpty()) {
            return;
        }

        // Check if slug exists in Articles
        articleRepository.findBySlug(slug).ifPresent(article -> {
            if (excludeArticleId == null || !article.getArticleId().equals(excludeArticleId)) {
                throw new IllegalArgumentException("Slug '" + slug + "' already exists");
            }
        });
    }

    /**
     * Validates that a slug is unique in Articles (for new entities).
     * 
     * @param slug the slug to validate
     * @throws IllegalArgumentException if the slug already exists
     */
    public void validateSlugUniqueness(String slug) {
        validateSlugUniqueness(slug, null);
    }
}

