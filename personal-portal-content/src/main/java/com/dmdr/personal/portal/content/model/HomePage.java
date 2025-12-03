package com.dmdr.personal.portal.content.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "home_page")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class HomePage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "home_page_id")
    @EqualsAndHashCode.Include
    private UUID homePageId;

    @Column(name = "welcome_message", columnDefinition = "TEXT")
    private String welcomeMessage;

    @Column(name = "welcome_media_id")
    private UUID welcomeMediaId;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "welcome_article_ids", columnDefinition = "UUID[]")
    private List<UUID> welcomeArticleIds = new ArrayList<>();

    @Column(name = "about_message", columnDefinition = "TEXT")
    private String aboutMessage;

    @Column(name = "about_media_id")
    private UUID aboutMediaId;

    @Column(name = "education_message", columnDefinition = "TEXT")
    private String educationMessage;

    @Column(name = "education_media_id")
    private UUID educationMediaId;

    @Column(name = "review_message", columnDefinition = "TEXT")
    private String reviewMessage;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "review_media_ids", columnDefinition = "UUID[]")
    private List<UUID> reviewMediaIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "home_page_contact", joinColumns = @JoinColumn(name = "home_page_id"))
    private List<Contact> contact = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

