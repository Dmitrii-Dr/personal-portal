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
@Table(name = "booking_settings")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BookingSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	// All intervals are in minutes

	@Column(name = "booking_slots_interval", nullable = false)
	private int bookingSlotsInterval;

	@Column(name = "booking_first_slot_interval", nullable = false)
	private int bookingFirstSlotInterval;

	@Column(name = "booking_cancelation_interval", nullable = false)
	private int bookingCancelationInterval;

	@Column(name = "booking_updating_interval", nullable = false)
	private int bookingUpdatingInterval;

	// Default timezone for availability rules (e.g., "13", "14")
	@Column(name = "default_timezone_id", nullable = false)
	private Integer defaultTimezoneId;

	// UTC offset for default timezone (e.g., "+00:00", "+05:30", "-05:00")
	@Column(name = "default_utc_offset", nullable = false, length = 10)
	private String defaultUtcOffset;
}
