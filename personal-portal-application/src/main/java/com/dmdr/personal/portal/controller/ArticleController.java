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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
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
    public ResponseEntity<List<ArticleResponse>> getPublicArticles() {
        List<Article> articles = articleService.findPublicArticles();
        List<ArticleResponse> responses = articles.stream()
                .map(ArticleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
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

