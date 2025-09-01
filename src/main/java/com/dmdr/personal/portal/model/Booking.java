package com.dmdr.personal.portal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "bookings", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bookings_slot", columnNames = "slot_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private User client;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private BookingSlot slot;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;
}
