package com.dmdr.personal.portal.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
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

    @Column(name = "about_message", columnDefinition = "TEXT")
    private String aboutMessage;

    @Column(name = "about_media_id")
    private UUID aboutMediaId;

    @Column(name = "education_message", columnDefinition = "TEXT")
    private String educationMessage;

    @Column(name = "education_media_id")
    private UUID educationMediaId;

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

