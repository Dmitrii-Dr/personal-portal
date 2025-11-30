package com.dmdr.personal.portal.booking.repository;

import com.dmdr.personal.portal.booking.model.AvailabilityRule;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    //TODO in most cases we will need to load only ACTIVE rule with overlap given new rule to compare working hours.
	List<AvailabilityRule> findByRuleStatus(AvailabilityRule.RuleStatus ruleStatus);

	@Query("SELECT ar FROM AvailabilityRule ar " +
		"WHERE ar.ruleStartInstant < :endInstant " +
		"AND ar.ruleEndInstant > :startInstant " +
		"AND (:excludeId IS NULL OR ar.id != :excludeId)")
	List<AvailabilityRule> findOverlappingRules(
		@Param("startInstant") Instant startInstant,
		@Param("endInstant") Instant endInstant,
		@Param("excludeId") Long excludeId
	);

	@Query("SELECT COUNT(ar) FROM AvailabilityRule ar " +
		"WHERE ar.ruleStatus != :archivedStatus")
	long countNonArchivedRules(@Param("archivedStatus") AvailabilityRule.RuleStatus archivedStatus);

	@Query("SELECT ar FROM AvailabilityRule ar " +
		"WHERE ar.ruleStatus = :status " +
		"AND ar.ruleStartInstant < :endInstant " +
		"AND ar.ruleEndInstant > :startInstant")
	List<AvailabilityRule> findActiveOverlappingRules(
		@Param("startInstant") Instant startInstant,
		@Param("endInstant") Instant endInstant,
		@Param("status") AvailabilityRule.RuleStatus status
	);

	@Query("SELECT ar FROM AvailabilityRule ar " +
		"WHERE ar.ruleStatus != :archivedStatus " +
		"AND (:excludeId IS NULL OR ar.id != :excludeId)")
	List<AvailabilityRule> findAllNonArchivedRules(
		@Param("archivedStatus") AvailabilityRule.RuleStatus archivedStatus,
		@Param("excludeId") Long excludeId
	);

	@Query("SELECT ar FROM AvailabilityRule ar " +
		"WHERE ar.ruleStatus = :status " +
		"AND ar.ruleEndInstant < :now")
	List<AvailabilityRule> findActiveRulesWithExpiredEndDate(
		@Param("status") AvailabilityRule.RuleStatus status,
		@Param("now") Instant now
	);
}


