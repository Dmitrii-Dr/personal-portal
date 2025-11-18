package com.dmdr.personal.portal.booking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "availability_rules")
@Getter
@Setter
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AvailabilityRule {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

    //Rule can be applied to multiple days of week
    //Stored as integer array where Monday = 1, Sunday = 7
    @Column(name = "days_of_week", nullable = false, columnDefinition = "INTEGER[]")
	private int[] daysOfWeekAsInt;

     //Start of working hours 
	@Column(name = "available_start_time", nullable = false)
	private LocalTime availableStartTime;

    //End of working hours
	@Column(name = "available_end_time", nullable = false)
	private LocalTime availableEndTime;

	// Defines the start of the time period when this rule is valid (in UTC)
	@Column(name = "rule_start_time")
	private Instant ruleStartInstant;

	// Defines the end of the time period when this rule is valid (in UTC)
	@Column(name = "rule_end_time")
	private Instant ruleEndInstant;

	// Timezone for this rule (e.g., "UTC", "America/New_York"). If null, uses default timezone from BookingSettings
	@Column(name = "timezone", length = 50)
	private String timezone;

	// UTC offset for this rule (e.g., "+00:00", "+05:30", "-05:00"). If null, uses default UTC offset from BookingSettings
	@Column(name = "utc_offset", length = 10)
	private String utcOffset;

	private RuleStatus ruleStatus;

// Rule can't be turn ACTIVE if there is another ACTIVE Rule which cross this one.
// When this.rule is switchig to ACTIVE we will find all ACTIVE rules where that.ruleStartInstant is before this.ruleEndInstant and that.ruleEndInstant is after this.ruleStartInstant
// We will iterrate over this set of Rules and filter those that.daysOfWeek containsAny this.daysOfWeek
// Then we filter those where  that.vailableStartTime is before this.availableEndTime and that.availableEndTime is after this.availableStartTime
// If there are no such rules, we can turn this rule to ACTIVE, otherwise we can't turn it to ACTIVE.

    public enum RuleStatus {
        ACTIVE,
        INACTIVE
    }
}


