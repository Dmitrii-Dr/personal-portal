-- REST request observability: persisted request log rows (UTC timestamps)

CREATE TABLE IF NOT EXISTS request_log (
    id BIGSERIAL PRIMARY KEY,
    path VARCHAR(2048) NOT NULL,
    template_path VARCHAR(2048) NOT NULL,
    method VARCHAR(16) NOT NULL,
    status INTEGER NOT NULL,
    duration_ms BIGINT NOT NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    error_code VARCHAR(128),
    error_message TEXT,
    request_body TEXT,
    request_headers TEXT,
    response_headers TEXT,
    stack_trace TEXT
);

CREATE INDEX IF NOT EXISTS idx_request_log_created_at ON request_log(created_at);
CREATE INDEX IF NOT EXISTS idx_request_log_template_path_created_at ON request_log(template_path, created_at);
CREATE INDEX IF NOT EXISTS idx_request_log_status_created_at ON request_log(status, created_at);
