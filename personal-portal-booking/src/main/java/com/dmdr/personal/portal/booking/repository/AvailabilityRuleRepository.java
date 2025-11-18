package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    //TODO in most cases we will need to load only ACTIVE rule with overlap given new rule to compare working hours.
	List<AvailabilityRule> findByRuleStatus(AvailabilityRule.RuleStatus ruleStatus);
}


