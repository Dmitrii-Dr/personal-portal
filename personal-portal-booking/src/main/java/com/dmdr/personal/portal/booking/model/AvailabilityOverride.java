package com.dmdr.personal.portal.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalTime;
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

	@Column(name = "override_date", nullable = false)
	private LocalDate overrideDate;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

    //If isAvailable is true, the availability is extended
    //If isAvailable is false, the availability is reduced
	@Column(name = "is_available", nullable = false)
	private boolean isAvailable;
}


