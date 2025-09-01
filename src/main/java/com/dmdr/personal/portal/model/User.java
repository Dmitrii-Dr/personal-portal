package com.dmdr.personal.portal.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "users", indexes = @Index(name = "uk_users_email", columnList = "email", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Column(nullable = false, length = 255)
    @NotBlank(message = "Password is required")
    private String password;

    @Column(name = "full_name", nullable = false, length = 255)
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2 and 255 characters")
    private String fullName;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Role is required")
    private String role; // e.g. ROLE_USER / ROLE_ADMIN

    @Column(name = "created_at")
    private Instant createdAt;

    @OneToMany(mappedBy = "author")
    private List<BlogPost> blogPosts;

    @OneToMany(mappedBy = "client")
    private List<Booking> bookings;
}
