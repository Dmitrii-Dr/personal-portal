package com.dmdr.personal.portal.content.service.impl;

import com.dmdr.personal.portal.content.dto.CreateArticleRequest;
import com.dmdr.personal.portal.content.dto.UpdateArticleRequest;
import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.model.ArticleStatus;
import com.dmdr.personal.portal.content.model.MediaEntity;
import com.dmdr.personal.portal.content.model.Tag;
import com.dmdr.personal.portal.content.repository.ArticleRepository;
import com.dmdr.personal.portal.content.repository.HomePageRepository;
import com.dmdr.personal.portal.content.service.ArticleService;
import com.dmdr.personal.portal.content.service.MediaService;
import com.dmdr.personal.portal.content.service.SlugValidationService;
import com.dmdr.personal.portal.content.service.TagService;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArticleServiceImpl implements ArticleService {
    private final ArticleRepository articleRepository;
    private final TagService tagService;
    private final MediaService mediaService;
    private final UserService userService;
    private final SlugValidationService slugValidationService;
    private final HomePageRepository homePageRepository;

    public ArticleServiceImpl(ArticleRepository articleRepository, TagService tagService,
                             MediaService mediaService, UserService userService,
                             SlugValidationService slugValidationService,
                             HomePageRepository homePageRepository) {
        this.articleRepository = articleRepository;
        this.tagService = tagService;
        this.mediaService = mediaService;
        this.userService = userService;
        this.slugValidationService = slugValidationService;
        this.homePageRepository = homePageRepository;
    }

    @Override
    @Transactional
    public Article createArticle(Article article) {
        // Validate slug uniqueness across all entities
        slugValidationService.validateSlugUniqueness(article.getSlug());
        return articleRepository.save(article);
    }

    @Override
    @Transactional
    public Article createArticle(CreateArticleRequest request, UUID authorId) {
        // Validate that allowed users cannot be set for PUBLISHED articles
        if (!CollectionUtils.isEmpty(request.getAllowedUserIds())
                && request.getStatus() == ArticleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Allowed users cannot be set for PUBLISHED articles");
        }

        // Validate slug uniqueness across all entities
        slugValidationService.validateSlugUniqueness(request.getSlug());

        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setSlug(request.getSlug());
        article.setContent(request.getContent());
        article.setExcerpt(request.getExcerpt());
        article.setStatus(request.getStatus());
        article.setAuthorId(authorId);
        article.setFeaturedImageId(request.getFeaturedImageId());

        Set<Tag> tags = getTagsIfProvided(request.getTagIds());
        if (tags != null) {
            article.setTags(tags);
        }

        // Use mediaIds from CreateArticleRequest to link media entities
        Set<MediaEntity> mediaFiles = getMediaFilesIfProvided(request.getMediaIds());
        if (mediaFiles != null) {
            article.setMediaFiles(mediaFiles);
        }

        Set<User> allowedUsers = getAllowedUsersIfProvided(request.getAllowedUserIds(), request.getStatus());
        if (allowedUsers != null) {
            article.setAllowedUsers(allowedUsers);
        }

        return createArticle(article);
    }

    private Set<Tag> getTagsIfProvided(Set<UUID> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) {
            return null;
        }
        List<Tag> tags = tagService.findByIds(tagIds);
        if (tags.size() != tagIds.size()) {
            Set<UUID> foundTagIds = tags.stream()
                    .map(Tag::getTagId)
                    .collect(Collectors.toSet());
            Set<UUID> missingTagIds = tagIds.stream()
                    .filter(id -> !foundTagIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Tags not found: " + missingTagIds);
        }
        return new HashSet<>(tags);
    }

    private Set<MediaEntity> getMediaFilesIfProvided(Set<UUID> mediaFileIds) {
        if (CollectionUtils.isEmpty(mediaFileIds)) {
            return null;
        }
        List<MediaEntity> mediaFiles = mediaService.findByIds(mediaFileIds);
        if (mediaFiles.size() != mediaFileIds.size()) {
            Set<UUID> foundMediaIds = mediaFiles.stream()
                    .map(MediaEntity::getMediaId)
                    .collect(Collectors.toSet());
            Set<UUID> missingMediaIds = mediaFileIds.stream()
                    .filter(id -> !foundMediaIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Media files not found: " + missingMediaIds);
        }
        return new HashSet<>(mediaFiles);
    }

    private Set<User> getAllowedUsersIfProvided(Set<UUID> allowedUserIds, ArticleStatus status) {
        if (CollectionUtils.isEmpty(allowedUserIds)) {
            return null;
        }
        if (status == ArticleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Allowed users cannot be set for PUBLISHED articles");
        }
        List<User> users = userService.findByIds(allowedUserIds);
        if (users.size() != allowedUserIds.size()) {
            Set<UUID> foundUserIds = users.stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
            Set<UUID> missingUserIds = allowedUserIds.stream()
                    .filter(id -> !foundUserIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Users not found: " + missingUserIds);
        }
        return new HashSet<>(users);
    }

    @Override
    public Optional<Article> findById(UUID articleId) {
        if (articleId == null) {
            return Optional.empty();
        }
        return articleRepository.findByArticleId(articleId);
    }

    @Override
    public Optional<Article> findBySlug(String slug) {
        return articleRepository.findBySlug(slug);
    }

    @Override
    public List<Article> findAll() {
        return articleRepository.findAll();
    }

    @Override
    public List<Article> findPublicArticles() {
        return articleRepository.findByStatus(ArticleStatus.PUBLISHED);
    }

    @Override
    public List<Article> findPrivateArticlesForUser(UUID userId) {
        if (userId == null) {
            return List.of();
        }
        return articleRepository.findByStatusAndAllowedUsers_Id(ArticleStatus.PRIVATE, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Article> findPublishedArticlesByIds(List<UUID> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        
        List<Article> articles = articleRepository.findByArticleIdIn(articleIds);
        
        // Validate that all requested articles were found
        if (articles.size() != articleIds.size()) {
            Set<UUID> foundIds = articles.stream()
                    .map(Article::getArticleId)
                    .collect(Collectors.toSet());
            Set<UUID> missingIds = articleIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Articles not found: " + missingIds);
        }
        
        // Validate that all articles have PUBLISHED status
        List<Article> nonPublishedArticles = articles.stream()
                .filter(article -> article.getStatus() != ArticleStatus.PUBLISHED)
                .collect(Collectors.toList());
        
        if (!nonPublishedArticles.isEmpty()) {
            Set<UUID> nonPublishedIds = nonPublishedArticles.stream()
                    .map(Article::getArticleId)
                    .collect(Collectors.toSet());
            throw new IllegalArgumentException("Articles are not PUBLISHED: " + nonPublishedIds);
        }
        
        return articles;
    }

    @Override
    @Transactional(readOnly = true)
    public Article findPublishedArticleById(UUID articleId) {
        Article article = articleRepository.findByArticleId(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article with id " + articleId + " not found"));
        
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Article with id " + articleId + " is not PUBLISHED");
        }
        
        return article;
    }

    @Override
    @Transactional(readOnly = true)
    public Article findPublishedArticleBySlug(String slug) {
        Article article = articleRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Article with slug '" + slug + "' not found"));
        
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Article with slug '" + slug + "' is not PUBLISHED");
        }
        
        return article;
    }


    @Override
    @Transactional
    public Article updateArticle(UUID articleId, UpdateArticleRequest request) {
        Article existingArticle = articleRepository.findByArticleId(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article with id " + articleId + " not found"));

        // Validate that allowed users cannot be set for PUBLISHED articles
        ArticleStatus newStatus = request.getStatus() != null ? request.getStatus() : existingArticle.getStatus();
        if (!CollectionUtils.isEmpty(request.getAllowedUserIds())
                && newStatus == ArticleStatus.PUBLISHED) {
            throw new IllegalArgumentException("Allowed users cannot be set for PUBLISHED articles");
        }
        if (ArticleStatus.isUnpublished(newStatus) && homePageRepository.existsByArticleId(articleId)) {
            throw new IllegalArgumentException(
                    "Cannot unpublish article with id " + articleId + " because it is being used by the home page");
        }

        // Check if slug is being changed and validate uniqueness
        if (request.getSlug() != null && !request.getSlug().equals(existingArticle.getSlug())) {
            slugValidationService.validateSlugUniqueness(request.getSlug(), existingArticle.getArticleId());
        }

        // Update fields
        if (request.getTitle() != null) {
            existingArticle.setTitle(request.getTitle());
        }
        if (request.getSlug() != null) {
            existingArticle.setSlug(request.getSlug());
        }
        if (request.getContent() != null) {
            existingArticle.setContent(request.getContent());
        }
        if (request.getExcerpt() != null) {
            existingArticle.setExcerpt(request.getExcerpt());
        }
        if (request.getStatus() != null) {
            existingArticle.setStatus(request.getStatus());
        }
        if (request.getFeaturedImageId() != null) {
            existingArticle.setFeaturedImageId(request.getFeaturedImageId());
        }

        // Update relationships if provided
        if (request.getTagIds() != null) {
            Set<Tag> tags = getTagsIfProvided(request.getTagIds());
            if (tags != null) {
                existingArticle.getTags().clear();
                existingArticle.getTags().addAll(tags);
            } else {
                existingArticle.getTags().clear();
            }
        }

        Set<UUID> removedMediaIds = new HashSet<>();
        if (request.getMediaIds() != null) {
            Set<UUID> existingMediaIds = existingArticle.getMediaFiles().stream()
                    .map(MediaEntity::getMediaId)
                    .collect(Collectors.toSet());
            Set<UUID> requestMediaIds = request.getMediaIds();
            
            // Find mediaIds that were removed (in existing but not in request)
            removedMediaIds = existingMediaIds.stream()
                    .filter(id -> !requestMediaIds.contains(id))
                    .collect(Collectors.toSet());
            
            // Remove relations for deleted media
            if (!removedMediaIds.isEmpty()) {
                List<MediaEntity> mediaToRemove = mediaService.findByIds(removedMediaIds);
                for (MediaEntity media : mediaToRemove) {
                    existingArticle.removeMediaFile(media);
                }
            }
            
            // Find mediaIds that are new (in request but not in existing)
            Set<UUID> newMediaIds = requestMediaIds.stream()
                    .filter(id -> !existingMediaIds.contains(id))
                    .collect(Collectors.toSet());
            
            // Add relations for new media
            if (!newMediaIds.isEmpty()) {
                Set<MediaEntity> newMediaFiles = getMediaFilesIfProvided(newMediaIds);
                if (newMediaFiles != null) {
                    for (MediaEntity media : newMediaFiles) {
                        existingArticle.addMediaFile(media);
            }
        }
            }
        }

        if(request.getStatus() == ArticleStatus.PUBLISHED) {
            existingArticle.getAllowedUsers().clear();
        }
        else if (request.getAllowedUserIds() != null) {
            Set<User> allowedUsers = getAllowedUsersIfProvided(request.getAllowedUserIds(), newStatus);
            if (allowedUsers != null) {
                existingArticle.getAllowedUsers().clear();
                existingArticle.getAllowedUsers().addAll(allowedUsers);
            } else {
                existingArticle.getAllowedUsers().clear();
            }
        }
        
        // Save the article to update relationships
        Article savedArticle = articleRepository.save(existingArticle);

        return savedArticle;
    }

    @Override
    @Transactional
    public void deleteArticle(UUID articleId) {
        if (articleId == null) {
            throw new IllegalArgumentException("Article id cannot be null");
        }
        // Verify article exists before deleting
        articleRepository.findByArticleId(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article with id " + articleId + " not found"));
        
        // Check if article is used by HomePage
        if (homePageRepository.existsByArticleId(articleId)) {
            throw new IllegalArgumentException(
                    "Cannot delete article with id " + articleId + " because it is being used by the home page");
        }
        
        // Delete the article
        articleRepository.deleteById(articleId);
    }

}


