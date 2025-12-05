package com.dmdr.personal.portal.content.repository;

import com.dmdr.personal.portal.content.model.HomePage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HomePageRepository extends JpaRepository<HomePage, UUID> {
    
    Optional<HomePage> findFirstByOrderByCreatedAtAsc();
    
    /**
     * Checks if a media ID is used in any field of the HomePage.
     * Checks welcomeMediaId, aboutMediaId, educationMediaId, and reviewMediaIds array.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM home_page h WHERE " +
           "h.welcome_media_id = :mediaId OR " +
           "h.about_media_id = :mediaId OR " +
           "h.education_media_id = :mediaId OR " +
           ":mediaId = ANY(h.review_media_ids)", nativeQuery = true)
    boolean existsByMediaId(@Param("mediaId") UUID mediaId);
    
    /**
     * Checks if an article ID is used in welcomeArticleIds array of the HomePage.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM home_page h WHERE :articleId = ANY(h.welcome_article_ids)", nativeQuery = true)
    boolean existsByArticleId(@Param("articleId") UUID articleId);
}

