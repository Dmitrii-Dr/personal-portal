package com.dmdr.personal.portal.content.service.impl;

import com.dmdr.personal.portal.content.dto.CreateArticleRequest;
import com.dmdr.personal.portal.content.dto.UpdateArticleRequest;
import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.model.ArticleStatus;
import com.dmdr.personal.portal.content.model.MediaEntity;
import com.dmdr.personal.portal.content.model.Tag;
import com.dmdr.personal.portal.content.repository.ArticleRepository;
import com.dmdr.personal.portal.content.service.ArticleService;
import com.dmdr.personal.portal.content.service.MediaService;
import com.dmdr.personal.portal.content.service.TagService;
import com.dmdr.personal.portal.content.service.s3.S3Service;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
    private final S3Service s3Service;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public ArticleServiceImpl(ArticleRepository articleRepository, TagService tagService,
                             MediaService mediaService, UserService userService, S3Service s3Service,
                             TransactionTemplate transactionTemplate) {
        this.articleRepository = articleRepository;
        this.tagService = tagService;
        this.mediaService = mediaService;
        this.userService = userService;
        this.s3Service = s3Service;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    @Transactional
    public Article createArticle(Article article) {
        // Check if article with slug already exists
        if (article.getSlug() != null && articleRepository.findBySlug(article.getSlug()).isPresent()) {
            throw new IllegalArgumentException("Article with slug " + article.getSlug() + " already exists");
        }
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

        // Check if slug is being changed and if the new slug already exists
        if (request.getSlug() != null && !request.getSlug().equals(existingArticle.getSlug())) {
            if (articleRepository.findBySlug(request.getSlug()).isPresent()) {
                throw new IllegalArgumentException("Article with slug " + request.getSlug() + " already exists");
            }
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
        
        // Check and delete orphaned media files in a virtual thread after transaction commits
        if (!removedMediaIds.isEmpty()) {
            Set<UUID> finalRemovedMediaIds = removedMediaIds;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    Thread.ofVirtual().start(() -> {
                        List<MediaEntity> removedMedia = mediaService.findByIds(finalRemovedMediaIds);
                        // Execute in a new transaction to avoid LazyInitializationException
                        transactionTemplate.execute(status -> {
                            deleteOrphanedMediaFiles(new HashSet<>(removedMedia));
                            return null;
                        });
                    });
                }
            });
        }

        return savedArticle;
    }

    @Override
    @Transactional
    public void deleteArticle(UUID articleId) {
        if (articleId == null) {
            throw new IllegalArgumentException("Article id cannot be null");
        }
        Article article = articleRepository.findByArticleId(articleId)
                .orElseThrow(() -> new IllegalArgumentException("Article with id " + articleId + " not found"));
        
        // Collect media entities before deleting the article
        Set<MediaEntity> mediaFiles = new HashSet<>(article.getMediaFiles());
        
        // Delete the article
        articleRepository.deleteById(articleId);
        
        // Find and delete orphaned media files
        deleteOrphanedMediaFiles(mediaFiles);
    }

    /**
     * Deletes media files that are no longer used by any article.
     * Also deletes the files from S3.
     * This method should be called within a transaction context.
     * 
     * @param mediaFiles Set of MediaEntity objects to check and potentially delete
     */
    private void deleteOrphanedMediaFiles(Set<MediaEntity> mediaFiles) {
        if (CollectionUtils.isEmpty(mediaFiles)) {
            return;
        }
        
        for (MediaEntity media : mediaFiles) {
            if (media == null) {
                continue;
            }
            
            // Check if media is orphaned using repository query
            // This avoids LazyInitializationException when called from virtual thread
            if (isMediaOrphaned(media)) {
                try {
                    // Delete from S3 first
                    s3Service.deleteFile(media.getFileUrl());
                    // Then delete from database
                    mediaService.deleteMedia(media.getMediaId());
                } catch (Exception e) {
                    // Log error but continue with other media files
                    System.err.println("Error deleting orphaned media file " + media.getMediaId() + ": " + e.getMessage());
                }
            }
        }
    }

    /**
     * Checks if a media file is orphaned (not used by any article).
     * Uses a repository query to check if any article references this media.
     */
    private boolean isMediaOrphaned(MediaEntity media) {
        if (media == null) {
            return false;
        }
        // Use repository query to check if media is used by any article
        // This avoids LazyInitializationException when called from virtual thread
        return !articleRepository.existsByMediaId(media.getMediaId());
    }
}


