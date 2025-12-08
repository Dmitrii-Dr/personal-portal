package com.dmdr.personal.portal.booking.service.impl;

import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import com.dmdr.personal.portal.booking.repository.AvailabilityOverrideRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AvailabilityOverrideArchiveScheduler {

	private static final Logger logger = LoggerFactory.getLogger(AvailabilityOverrideArchiveScheduler.class);

	private final AvailabilityOverrideRepository repository;

	public AvailabilityOverrideArchiveScheduler(AvailabilityOverrideRepository repository) {
		this.repository = repository;
	}

	/**
	 * Archives expired ACTIVE AvailabilityOverrides at startup and then every hour.
	 * An override is considered expired when its overrideEndInstant is in the past.
	 */
	@Scheduled(fixedRate = 60 * 60 * 1000, initialDelay = 0) // 1 hour in milliseconds, run immediately at startup
	@Transactional
	public void archiveExpiredOverrides() {
		Instant now = Instant.now();
		List<AvailabilityOverride> expiredOverrides = repository.findActiveOverridesWithExpiredEndDate(
			OverrideStatus.ACTIVE,
			now
		);

		if (expiredOverrides.isEmpty()) {
			logger.info("No expired ACTIVE AvailabilityOverrides found to archive");
			return;
		}

		int archivedCount = 0;
		for (AvailabilityOverride override : expiredOverrides) {
			override.setOverrideStatus(OverrideStatus.ARCHIVED);
			repository.save(override);
			archivedCount++;
			logger.info("Archived expired AvailabilityOverride with ID: {}, overrideEndInstant: {}", 
				override.getId(), override.getOverrideEndInstant());
		}

		logger.info("Successfully archived {} expired AvailabilityOverride(s)", archivedCount);
	}
}

