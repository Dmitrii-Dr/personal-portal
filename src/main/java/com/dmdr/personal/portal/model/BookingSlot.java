package com.dmdr.personal.portal.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "booking_slots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingSlot {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "is_booked", nullable = false)
    private boolean booked;
}
