package com.dmdr.personal.portal.service.impl;

import com.dmdr.personal.portal.model.BlogPost;
import com.dmdr.personal.portal.repository.BlogPostRepository;
import com.dmdr.personal.portal.service.BlogPostService;
import com.dmdr.personal.portal.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogPostServiceImpl implements BlogPostService {

    private final BlogPostRepository repository;

    @Override
    public List<BlogPost> listPublished() {
        return repository.findByPublishedAtNotNullOrderByPublishedAtDesc();
    }

    @Override
    public Optional<BlogPost> findBySlug(String slug) {
        return repository.findBySlug(slug);
    }

    @Override
    public BlogPost save(BlogPost post) {
        if (post.getId() == null) {
            post.setCreatedAt(Instant.now());
        }
        if (post.getSlug() == null || post.getSlug().isBlank()) {
            post.setSlug(SlugUtil.toSlug(post.getTitle()));
        }
        return repository.save(post);
    }
}
