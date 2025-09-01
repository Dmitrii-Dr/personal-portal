package com.dmdr.personal.portal.service;

import com.dmdr.personal.portal.model.BlogPost;

import java.util.List;
import java.util.Optional;

public interface BlogPostService {
    List<BlogPost> listPublished();
    Optional<BlogPost> findBySlug(String slug);
    BlogPost save(BlogPost post);
}
