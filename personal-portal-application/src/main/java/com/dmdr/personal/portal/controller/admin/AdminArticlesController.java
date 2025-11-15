package com.dmdr.personal.portal.controller.admin;

import com.dmdr.personal.portal.content.dto.ArticleMapper;
import com.dmdr.personal.portal.content.dto.ArticleResponse;
import com.dmdr.personal.portal.content.dto.CreateArticleRequest;
import com.dmdr.personal.portal.content.dto.UpdateArticleRequest;
import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.service.ArticleService;
import com.dmdr.personal.portal.controller.CurrentUserService;
import com.dmdr.personal.portal.users.model.User;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/articles")
@Slf4j
public class AdminArticlesController {

    private final ArticleService articleService;
    private final CurrentUserService currentUserService;

    public AdminArticlesController(ArticleService articleService, CurrentUserService currentUserService) {
        this.articleService = articleService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<List<ArticleResponse>> getAllArticles() {
        List<Article> articles = articleService.findAll();
        List<ArticleResponse> responses = articles.stream()
                .map(ArticleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        User author = currentUserService.getCurrentUser();
        Article createdArticle = articleService.createArticle(request, author.getId());
        ArticleResponse response = ArticleMapper.toResponse(createdArticle);

        log.info("Article created successfully by admin: {}", createdArticle.getSlug());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable("articleId") UUID articleId,
            @Valid @RequestBody UpdateArticleRequest request) {
        Article updatedArticle = articleService.updateArticle(articleId, request);
        ArticleResponse response = ArticleMapper.toResponse(updatedArticle);

        log.info("Article updated successfully by admin: {}", updatedArticle.getSlug());
        return ResponseEntity.ok(response);
    }
}

