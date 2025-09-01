package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.model.BlogPost;
import com.dmdr.personal.portal.repository.BlogPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class BlogPostInitializer implements CommandLineRunner {

    private final BlogPostRepository repository;

    @Override
    @Transactional
    public void run(String... args) {
        boolean anyPublished = !repository.findByPublishedAtNotNullOrderByPublishedAtDesc().isEmpty();
        if (anyPublished) return;

        String content = "Psychology explores how people think, feel, and behave. Small changes in habits, environment, and mindset shape long-term wellbeing. Understanding cognitive biases and emotional regulation helps us make better decisions, build stronger relationships, and reduce stress. Curiosity, reflection, and compassionate self-observation are practical tools for personal growth and resilience for change.";

        BlogPost post = BlogPost.builder()
                .title("Psychology in Everyday Life")
                .slug("psychology-everyday-life")
                .content(content)
                .createdAt(Instant.now())
                .publishedAt(Instant.now())
                .build();

        repository.save(post);
    }
}
