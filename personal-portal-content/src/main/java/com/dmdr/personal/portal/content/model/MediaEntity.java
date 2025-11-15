package com.dmdr.personal.portal.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "media")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MediaEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "media_id")
    @EqualsAndHashCode.Include
    private UUID mediaId;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "uploaded_by_id", nullable = false)
    private UUID uploadedById;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToMany(mappedBy = "mediaFiles", fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Article> articles = new HashSet<>();

    @PrePersist
    public void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public void addArticle(Article article) {
        this.articles.add(article);
        article.getMediaFiles().add(this);
    }

    public void removeArticle(Article article) {
        this.articles.remove(article);
        article.getMediaFiles().remove(this);
    }
}

