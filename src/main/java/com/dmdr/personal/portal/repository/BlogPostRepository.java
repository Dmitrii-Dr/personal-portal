package com.dmdr.personal.portal.repository;

import com.dmdr.personal.portal.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    Optional<BlogPost> findBySlug(String slug);
    List<BlogPost> findByPublishedAtNotNullOrderByPublishedAtDesc();
}
