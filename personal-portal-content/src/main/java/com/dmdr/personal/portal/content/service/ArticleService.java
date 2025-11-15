package com.dmdr.personal.portal.content.service;

import com.dmdr.personal.portal.content.dto.CreateArticleRequest;
import com.dmdr.personal.portal.content.dto.UpdateArticleRequest;
import com.dmdr.personal.portal.content.model.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleService {

    Article createArticle(Article article);

    Article createArticle(CreateArticleRequest request, UUID authorId);

    Optional<Article> findById(UUID articleId);

    Optional<Article> findBySlug(String slug);

    List<Article> findAll();

    List<Article> findPublicArticles();

    List<Article> findPrivateArticlesForUser(UUID userId);

    Article updateArticle(UUID articleId, UpdateArticleRequest request);

    void deleteArticle(UUID articleId);

}

