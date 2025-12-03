package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.content.dto.ArticleMapper;
import com.dmdr.personal.portal.content.dto.ArticleResponse;
import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.service.ArticleService;
import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.users.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class ArticleController {

    private final ArticleService articleService;
    private final CurrentUserService currentUserService;

    public ArticleController(ArticleService articleService, CurrentUserService currentUserService) {
        this.articleService = articleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/public/articles")
    public ResponseEntity<List<ArticleResponse>> getPublicArticles(
            @RequestParam(required = false) String id) {
        if (id != null && !id.isEmpty()) {
            // Parse comma-separated UUIDs
            List<UUID> articleIds = Arrays.stream(id.split(","))
                    .map(String::trim)
                    .map(UUID::fromString)
                    .collect(Collectors.toList());
            
            List<Article> articles = articleService.findPublishedArticlesByIds(articleIds);
            List<ArticleResponse> responses = articles.stream()
                    .map(ArticleMapper::toResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        }
        
        // Default behavior: return all public articles
        List<Article> articles = articleService.findPublicArticles();
        List<ArticleResponse> responses = articles.stream()
                .map(ArticleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/public/articles/{articleId}")
    public ResponseEntity<ArticleResponse> getPublicArticleById(@PathVariable UUID articleId) {
        Article article = articleService.findPublishedArticleById(articleId);
        ArticleResponse response = ArticleMapper.toResponse(article);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/articles/slug/{slug}")
    public ResponseEntity<ArticleResponse> getPublicArticleBySlug(@PathVariable String slug) {
        Article article = articleService.findPublishedArticleBySlug(slug);
        ArticleResponse response = ArticleMapper.toResponse(article);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/articles")
    public ResponseEntity<List<ArticleResponse>> getPrivateArticles() {
        User user = currentUserService.getCurrentUser();
        List<Article> articles = articleService.findPrivateArticlesForUser(user.getId());
        List<ArticleResponse> responses = articles.stream()
                .map(ArticleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

