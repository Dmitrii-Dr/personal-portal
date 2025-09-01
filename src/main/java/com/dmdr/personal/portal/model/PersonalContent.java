package com.dmdr.personal.portal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "personal_content")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonalContent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    private String description;

    @Column(name = "content_url", length = 255)
    private String contentUrl;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "created_at")
    private Instant createdAt;
}
