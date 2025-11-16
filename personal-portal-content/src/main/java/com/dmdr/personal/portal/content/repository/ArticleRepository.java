package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.model.ArticleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findByArticleId(UUID articleId);

    List<Article> findByStatusAndAllowedUsers_Id(ArticleStatus status, UUID userId);

    List<Article> findByStatus(ArticleStatus status);

}

