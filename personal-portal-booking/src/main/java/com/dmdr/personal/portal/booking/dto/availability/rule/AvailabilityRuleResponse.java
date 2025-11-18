package com.dmdr.personal.portal.booking.dto.availability.rule;

import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityRuleResponse {
	private Long id;
	private List<DayOfWeek> daysOfWeek;
	private LocalTime availableStartTime;
	private LocalTime availableEndTime;
	private LocalDate ruleStartDate;
	private LocalDate ruleEndDate;
	private String timezone;
	private String utcOffset;
	private AvailabilityRule.RuleStatus ruleStatus;
}


