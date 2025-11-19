package com.dmdr.personal.portal.booking.dto.availability.rule;

import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAvailabilityRuleRequest {
	@NotNull
	private Long id;
	@NotEmpty
	private List<DayOfWeek> daysOfWeek;
	@NotNull
	private LocalTime availableStartTime;
	@NotNull
	private LocalTime availableEndTime;
	@NotNull
	private LocalDate ruleStartDate;
	@NotNull
	private LocalDate ruleEndDate;
	@NotNull
	private AvailabilityRule.RuleStatus ruleStatus;
}


