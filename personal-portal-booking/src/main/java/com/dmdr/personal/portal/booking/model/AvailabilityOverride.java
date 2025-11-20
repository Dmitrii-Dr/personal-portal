package com.dmdr.personal.portal.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "availability_overrides")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AvailabilityOverride {
    //When creating AvailabilityOverride we will find main AvailaviltyRyles which cross this override
    //If this.isAvailable == true and there are no crossing rules, we can create this override, otherwise we can't create it.
    //If this.isAvailable == false and there are crossing rules, we can create this override, otherwise we can't create it.
    
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

    //If isAvailable is true, the availability is extended
    //If isAvailable is false, the availability is reduced
	@Column(name = "is_available", nullable = false)
	private boolean isAvailable;

	//overrideEndInstant minus overrideStartInstant can't be more than 24 hours. Override is allowed only within 24 hours.
	@Column(name = "override_start_time", nullable = false)
	private Instant overideStartInstant;

	@Column(name = "override_end_time", nullable = false)
	private Instant overrideEndInstant;

    @Column(name = "timezone", length = 50)
	private String timezone;

	// UTC offset for this rule (e.g., "+00:00", "+05:30", "-05:00"). If null, uses default UTC offset from BookingSettings
	@Column(name = "utc_offset", length = 10)
	private String utcOffset;

	@Enumerated(EnumType.STRING)
	@Column(name = "override_status", nullable = false, length = 50)
	private OverrideStatus overrideStatus;

}


