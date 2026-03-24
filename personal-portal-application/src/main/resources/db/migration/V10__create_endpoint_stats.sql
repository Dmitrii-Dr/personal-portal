-- Daily rollup: per UTC calendar day, method, and MVC template path (observability)

CREATE TABLE IF NOT EXISTS endpoint_stats (
    bucket_start DATE NOT NULL,
    method VARCHAR(16) NOT NULL,
    template_path VARCHAR(2048) NOT NULL,
    total_count BIGINT NOT NULL DEFAULT 0,
    success_count BIGINT NOT NULL DEFAULT 0,
    auth_error_count BIGINT NOT NULL DEFAULT 0,
    client_error_count BIGINT NOT NULL DEFAULT 0,
    server_error_count BIGINT NOT NULL DEFAULT 0,
    other_non_success_count BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (bucket_start, method, template_path)
);

CREATE INDEX IF NOT EXISTS idx_endpoint_stats_bucket_start_template_path ON endpoint_stats(bucket_start, template_path);
