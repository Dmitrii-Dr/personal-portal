package com.dmdr.personal.portal.admin.observability.repository;

import com.dmdr.personal.portal.admin.observability.model.CheckpointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ObservabilityRollupCheckpointRepository extends JpaRepository<CheckpointEntity, String> {
}
