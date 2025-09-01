package com.dmdr.personal.portal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "blog_posts", indexes = {
        @Index(name = "uk_blog_posts_slug", columnList = "slug", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BlogPost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;

    @Column(nullable = false, length = 255, unique = true)
    @NotBlank(message = "Slug is required")
    @Size(max = 255, message = "Slug must be less than 255 characters")
    private String slug;

    @Lob
    @Column(nullable = false)
    @NotBlank(message = "Content is required")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    public boolean isPublished() {
        return publishedAt != null;
    }
}
