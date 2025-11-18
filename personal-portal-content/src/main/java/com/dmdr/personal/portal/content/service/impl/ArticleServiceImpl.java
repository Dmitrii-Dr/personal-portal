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

    public ArticleServiceImpl(ArticleRepository articleRepository, TagService tagService,
                             MediaService mediaService, UserService userService) {
        this.articleRepository = articleRepository;
        this.tagService = tagService;
        this.mediaService = mediaService;
        this.userService = userService;
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

        Set<MediaEntity> mediaFiles = getMediaFilesIfProvided(request.getMediaFileIds());
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

        if (request.getMediaFileIds() != null) {
            Set<MediaEntity> mediaFiles = getMediaFilesIfProvided(request.getMediaFileIds());
            if (mediaFiles != null) {
                existingArticle.getMediaFiles().clear();
                existingArticle.getMediaFiles().addAll(mediaFiles);
            } else {
                existingArticle.getMediaFiles().clear();
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
        
        

        return articleRepository.save(existingArticle);
    }

    @Override
    @Transactional
    public void deleteArticle(UUID articleId) {
        if (articleId == null) {
            throw new IllegalArgumentException("Article id cannot be null");
        }
        if (!articleRepository.existsById(articleId)) {
            throw new IllegalArgumentException("Article with id " + articleId + " not found");
        }
        articleRepository.deleteById(articleId);
    }
}

