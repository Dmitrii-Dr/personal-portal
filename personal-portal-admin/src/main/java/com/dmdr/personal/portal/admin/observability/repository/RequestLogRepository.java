package com.dmdr.personal.portal.admin.observability.repository;

import com.dmdr.personal.portal.admin.observability.model.RequestLogEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestLogRepository extends JpaRepository<RequestLogEntity, Long>, JpaSpecificationExecutor<RequestLogEntity> {

    @Modifying
    @Query(
        value = """
            DELETE FROM request_log rl
            WHERE rl.ctid IN (
                SELECT ctid
                FROM request_log
                WHERE status >= 200 AND status < 300 AND created_at < :cutoff
                ORDER BY created_at
                LIMIT :batchSize
            )
            """,
        nativeQuery = true
    )
    int deleteSuccessRowsOlderThanBatch(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    @Modifying
    @Query(
        value = """
            DELETE FROM request_log rl
            WHERE rl.ctid IN (
                SELECT ctid
                FROM request_log
                WHERE status >= 300 AND created_at < :cutoff
                ORDER BY created_at
                LIMIT :batchSize
            )
            """,
        nativeQuery = true
    )
    int deleteFailureRowsOlderThanBatchHighStatus(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    @Modifying
    @Query(
        value = """
            DELETE FROM request_log rl
            WHERE rl.ctid IN (
                SELECT ctid
                FROM request_log
                WHERE status < 200 AND created_at < :cutoff
                ORDER BY created_at
                LIMIT :batchSize
            )
            """,
        nativeQuery = true
    )
    int deleteFailureRowsOlderThanBatchLowStatus(@Param("cutoff") Instant cutoff, @Param("batchSize") int batchSize);

    List<RequestLogEntity> findByCreatedAtBetween(Instant from, Instant to);

    List<RequestLogEntity> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);
}
