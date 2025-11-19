package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.AvailabilityOverride;
import com.dmdr.personal.portal.booking.model.OverrideStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityOverrideRepository extends JpaRepository<AvailabilityOverride, Long> {

	@Query("SELECT ao FROM AvailabilityOverride ao " +
		"WHERE ao.overrideStatus = :status " +
		"AND ao.overideStartInstant < :endInstant " +
		"AND ao.overrideEndInstant > :startInstant " +
		"AND (:excludeId IS NULL OR ao.id != :excludeId)")
	List<AvailabilityOverride> findOverlappingActiveOverrides(
		@Param("startInstant") Instant startInstant,
		@Param("endInstant") Instant endInstant,
		@Param("status") OverrideStatus status,
		@Param("excludeId") Long excludeId
	);

	@Query("SELECT COUNT(ao) FROM AvailabilityOverride ao " +
		"WHERE ao.overrideStatus != :archivedStatus")
	long countNonArchivedOverrides(@Param("archivedStatus") OverrideStatus archivedStatus);

	@Query("SELECT ao FROM AvailabilityOverride ao " +
		"WHERE ao.overrideStatus = :status " +
		"AND ao.isAvailable = true " +
		"AND ao.overideStartInstant < :endInstant " +
		"AND ao.overrideEndInstant > :startInstant")
	List<AvailabilityOverride> findOverlappingAvailableOverrides(
		@Param("startInstant") Instant startInstant,
		@Param("endInstant") Instant endInstant,
		@Param("status") OverrideStatus status
	);

	@Query("SELECT ao FROM AvailabilityOverride ao " +
		"WHERE ao.overrideStatus = :status " +
		"AND ao.isAvailable = false " +
		"AND ao.overideStartInstant < :endInstant " +
		"AND ao.overrideEndInstant > :startInstant")
	List<AvailabilityOverride> findOverlappingUnavailableOverrides(
		@Param("startInstant") Instant startInstant,
		@Param("endInstant") Instant endInstant,
		@Param("status") OverrideStatus status
	);
}

