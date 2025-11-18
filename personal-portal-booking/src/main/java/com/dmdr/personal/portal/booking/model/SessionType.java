package com.dmdr.personal.portal.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "session_types")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SessionType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	@Column(nullable = false, unique = true, length = 200)
	private String name;

    @Column(nullable = false, length = 2000)
	private String description;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

    //Buffer time after the session
    @Column(name = "buffer_minutes", nullable = false)
    private int bufferMinutes;

}


