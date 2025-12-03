package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.Article;
import com.dmdr.personal.portal.content.model.ArticleStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findByArticleId(UUID articleId);

    List<Article> findByStatusAndAllowedUsers_Id(ArticleStatus status, UUID userId);

    List<Article> findByStatus(ArticleStatus status);

    @Query("SELECT COUNT(a) > 0 FROM Article a JOIN a.mediaFiles m WHERE m.mediaId = :mediaId")
    boolean existsByMediaId(@Param("mediaId") UUID mediaId);

    List<Article> findByArticleIdIn(List<UUID> articleIds);

}

