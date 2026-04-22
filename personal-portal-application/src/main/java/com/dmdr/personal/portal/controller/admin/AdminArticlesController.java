package com.dmdr.personal.portal.controller.admin;
import com.dmdr.personal.portal.content.dto.AdminArticleResponse;
import com.dmdr.personal.portal.content.dto.ArticleMapper;
import com.dmdr.personal.portal.content.dto.ArticleResponse;
import com.dmdr.personal.portal.content.dto.CreateArticleRequest;
import com.dmdr.personal.portal.content.dto.UserResponse;
import com.dmdr.personal.portal.content.dto.UpdateArticleRequest;
import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.service.ArticleService;
import com.dmdr.personal.portal.service.CurrentUserService;
import com.dmdr.personal.portal.users.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    public ResponseEntity<List<AdminArticleResponse>> getAllArticles(HttpServletRequest httpRequest) {
        String ctx = AdminApiLogSupport.http(httpRequest);
        log.info("BEGIN getAllArticles {}", ctx);
        try {
        List<Article> articles = articleService.findAll();
        // Comparator like in AdminUserController.getUsers (case-insensitive by lastName, null-safe)
        java.util.Comparator<User> comparator = java.util.Comparator.comparing(
                u -> u.getLastName() == null ? "" : u.getLastName(),
                String.CASE_INSENSITIVE_ORDER
        );

        //TODO refactor this mapping. Move out from controller
        List<AdminArticleResponse> responses = articles.stream()
                .map(a -> {
                    // Build per-article users list from allowedUsers with ROLE_USER only
                    List<UserResponse> userDtos = a.getAllowedUsers().stream()
                            .sorted(comparator)
                            .map(u -> {
                                UserResponse dto = new UserResponse();
                                dto.setId(u.getId());
                                dto.setEmail(u.getEmail());
                                dto.setFirstName(u.getFirstName());
                                dto.setLastName(u.getLastName());
                                dto.setVerified(u.isActive());
                                dto.setCreatedAt(u.getCreatedAt());
                                dto.setUpdatedAt(u.getUpdatedAt());
                                return dto;
                            })
                            .collect(java.util.stream.Collectors.toList());

                    ArticleResponse base = ArticleMapper.toResponse(a);
                    AdminArticleResponse admin = new AdminArticleResponse();
                    admin.setArticleId(base.getArticleId());
                    admin.setTitle(base.getTitle());
                    admin.setSlug(base.getSlug());
                    admin.setContent(base.getContent());
                    admin.setExcerpt(base.getExcerpt());
                    admin.setStatus(base.getStatus());
                    admin.setAuthorId(base.getAuthorId());
                    admin.setFeaturedImageId(base.getFeaturedImageId());
                    admin.setCreatedAt(base.getCreatedAt());
                    admin.setUpdatedAt(base.getUpdatedAt());
                    admin.setPublishedAt(base.getPublishedAt());
                    admin.setTags(base.getTags());
                    admin.setMediaFileIds(base.getMediaFileIds());
                    admin.setUsers(userDtos);
                    return admin;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
        } finally {
            log.info("END getAllArticles {}", ctx);
        }
    }

    @PostMapping
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        int titleLen = request.getTitle() != null ? request.getTitle().length() : 0;
        int slugLen = request.getSlug() != null ? request.getSlug().length() : 0;
        int mediaIdsCount = request.getMediaIds() != null ? request.getMediaIds().size() : 0;
        String ctx = "status=" + request.getStatus()
            + " titleLength=" + titleLen
            + " slugLength=" + slugLen
            + " mediaIdsCount=" + mediaIdsCount;
        log.info("BEGIN createArticle {}", ctx);
        try {
            User author = currentUserService.getCurrentUser();
            Article createdArticle = articleService.createArticle(request, author.getId());
            ArticleResponse response = ArticleMapper.toResponse(createdArticle);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } finally {
            log.info("END createArticle {}", ctx);
        }
    }

    @PutMapping("/{articleId}")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable("articleId") UUID articleId,
            @Valid @RequestBody UpdateArticleRequest request) {
        int titleLen = request.getTitle() != null ? request.getTitle().length() : 0;
        String ctx = "articleId=" + articleId
            + " status=" + request.getStatus()
            + " titleLength=" + titleLen;
        log.info("BEGIN updateArticle {}", ctx);
        try {
            Article updatedArticle = articleService.updateArticle(articleId, request);
            ArticleResponse response = ArticleMapper.toResponse(updatedArticle);
            return ResponseEntity.ok(response);
        } finally {
            log.info("END updateArticle {}", ctx);
        }
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable("articleId") UUID articleId) {
        String ctx = "articleId=" + articleId;
        log.info("BEGIN deleteArticle {}", ctx);
        try {
            articleService.deleteArticle(articleId);
            return ResponseEntity.noContent().build();
        } finally {
            log.info("END deleteArticle {}", ctx);
        }
    }
}
