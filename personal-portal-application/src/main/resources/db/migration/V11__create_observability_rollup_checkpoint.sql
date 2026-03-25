-- Rollup checkpoint cursor for endpoint_stats aggregation job

CREATE TABLE IF NOT EXISTS portal_checkpoint (
    job_name VARCHAR(128) PRIMARY KEY,
    last_processed_request_log_id BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL
);
