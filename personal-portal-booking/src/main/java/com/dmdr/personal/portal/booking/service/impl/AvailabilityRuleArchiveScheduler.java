package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import com.dmdr.personal.portal.booking.repository.AvailabilityRuleRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AvailabilityRuleArchiveScheduler {

	private static final Logger logger = LoggerFactory.getLogger(AvailabilityRuleArchiveScheduler.class);

	private final AvailabilityRuleRepository repository;

	public AvailabilityRuleArchiveScheduler(AvailabilityRuleRepository repository) {
		this.repository = repository;
	}

	/**
	 * Archives expired ACTIVE AvailabilityRules at startup and then every 12 hours.
	 * A rule is considered expired when its ruleEndInstant is in the past.
	 */
	@Scheduled(fixedRate = 12 * 60 * 60 * 1000, initialDelay = 0) // 12 hours in milliseconds, run immediately at startup
	@Transactional
	public void archiveExpiredRules() {
		Instant now = Instant.now();
		List<AvailabilityRule> expiredRules = repository.findActiveRulesWithExpiredEndDate(
			AvailabilityRule.RuleStatus.ACTIVE,
			now
		);

		if (expiredRules.isEmpty()) {
			logger.info("No expired ACTIVE AvailabilityRules found to archive");
			return;
		}

		int archivedCount = 0;
		for (AvailabilityRule rule : expiredRules) {
			rule.setRuleStatus(AvailabilityRule.RuleStatus.ARCHIVED);
			repository.save(rule);
			archivedCount++;
			logger.info("Archived expired AvailabilityRule with ID: {}, ruleEndInstant: {}", 
				rule.getId(), rule.getRuleEndInstant());
		}

		logger.info("Successfully archived {} expired AvailabilityRule(s)", archivedCount);
	}
}

