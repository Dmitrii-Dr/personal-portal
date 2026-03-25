-- Seed deterministic observability test data for the last 3 months
-- Reference date: 2026-03-24 (UTC)
--
-- What this script inserts:
-- 1) request_log rows with mixed success and error outcomes
-- 2) endpoint_stats daily aggregates matching those request_log rows
--
-- Covered period: [2025-12-24, 2026-03-24] inclusive
-- Covered APIs (template_path + method):
--   GET    /api/v1/public/articles
--   GET    /api/v1/public/articles/{id}
--   GET    /api/v1/public/users/{userId}/profile
--   POST   /api/v1/public/booking
--   POST   /api/v1/public/contact
--   POST   /api/v1/auth/login
--   PUT    /api/v1/public/users/{userId}/profile
--   DELETE /api/v1/public/booking/{id}
--
-- Notes:
-- - Uses only synthetic user UUIDs and fake payload/header values.
-- - Safe to rerun: removes prior seed rows for the same period + paths.

BEGIN;

-- Optional cleanup for deterministic reruns.
DELETE FROM request_log
WHERE created_at >= TIMESTAMPTZ '2025-12-24T00:00:00Z'
  AND created_at <  TIMESTAMPTZ '2026-03-25T00:00:00Z'
  AND template_path IN (
      '/api/v1/public/articles',
      '/api/v1/public/articles/{id}',
      '/api/v1/public/users/{userId}/profile',
      '/api/v1/public/booking',
      '/api/v1/public/contact',
      '/api/v1/auth/login',
      '/api/v1/public/booking/{id}'
  );

DELETE FROM endpoint_stats
WHERE bucket_start >= DATE '2025-12-24'
  AND bucket_start <= DATE '2026-03-24'
  AND template_path IN (
      '/api/v1/public/articles',
      '/api/v1/public/articles/{id}',
      '/api/v1/public/users/{userId}/profile',
      '/api/v1/public/booking',
      '/api/v1/public/contact',
      '/api/v1/auth/login',
      '/api/v1/public/booking/{id}'
  );

WITH
seed_users AS (
    SELECT
        u.ids[1] AS user_1,
        u.ids[2] AS user_2,
        u.ids[3] AS user_3
    FROM (
        SELECT array_agg(id ORDER BY id) AS ids
        FROM (
            SELECT id
            FROM users
            ORDER BY id
            LIMIT 3
        ) s
    ) u
),
date_span AS (
    SELECT gs::date AS bucket_start
    FROM generate_series(DATE '2025-12-24', DATE '2026-03-24', INTERVAL '1 day') gs
),
endpoints AS (
    -- severity controls baseline error-proneness (higher => more errors).
    SELECT *
    FROM (
        VALUES
            ('GET'::varchar(16),    '/api/v1/public/articles'::varchar(2048),                90, 1),
            ('GET'::varchar(16),    '/api/v1/public/articles/{id}'::varchar(2048),           72, 2),
            ('GET'::varchar(16),    '/api/v1/public/users/{userId}/profile'::varchar(2048),  40, 3),
            ('POST'::varchar(16),   '/api/v1/public/booking'::varchar(2048),                  32, 4),
            ('POST'::varchar(16),   '/api/v1/public/contact'::varchar(2048),                  26, 5),
            ('POST'::varchar(16),   '/api/v1/auth/login'::varchar(2048),                      56, 7),
            ('PUT'::varchar(16),    '/api/v1/public/users/{userId}/profile'::varchar(2048),  20, 6),
            ('DELETE'::varchar(16), '/api/v1/public/booking/{id}'::varchar(2048),             14, 8)
    ) AS e(method, template_path, base_traffic, severity)
),
daily_rollup AS (
    SELECT
        d.bucket_start,
        e.method,
        e.template_path,
        -- Total volume with weekday/weekend and monthly variation.
        GREATEST(
            6,
            (
                e.base_traffic
                + CASE WHEN EXTRACT(ISODOW FROM d.bucket_start) IN (6, 7) THEN -12 ELSE 9 END
                + CASE WHEN EXTRACT(DAY FROM d.bucket_start) BETWEEN 1 AND 5 THEN 7 ELSE 0 END
                + ((EXTRACT(DOY FROM d.bucket_start)::int + e.severity * 3) % 11) - 5
            )
        )::bigint AS total_count,
        e.severity
    FROM date_span d
    CROSS JOIN endpoints e
),
daily_buckets AS (
    SELECT
        bucket_start,
        method,
        template_path,
        total_count,
        -- Error model by endpoint type:
        -- - login has higher auth errors
        -- - write endpoints have more client/server errors
        -- - read endpoints mostly successful with occasional client/server errors
        CASE
            WHEN template_path = '/api/v1/auth/login'
                THEN GREATEST(1, FLOOR(total_count * (0.14 + ((EXTRACT(DAY FROM bucket_start)::int % 5) * 0.01)))::bigint)
            ELSE FLOOR(total_count * (0.01 + (severity * 0.001)))::bigint
        END AS auth_error_count,
        CASE
            WHEN method IN ('POST', 'PUT', 'DELETE')
                THEN FLOOR(total_count * (0.10 + (severity * 0.004)))::bigint
            ELSE FLOOR(total_count * (0.05 + (severity * 0.002)))::bigint
        END AS client_error_count,
        CASE
            WHEN method IN ('POST', 'PUT', 'DELETE')
                THEN FLOOR(total_count * (0.06 + ((EXTRACT(DAY FROM bucket_start)::int % 7) * 0.003)))::bigint
            ELSE FLOOR(total_count * (0.03 + ((EXTRACT(DAY FROM bucket_start)::int % 9) * 0.002)))::bigint
        END AS server_error_count,
        CASE
            WHEN template_path = '/api/v1/auth/login'
                THEN FLOOR(total_count * 0.01)::bigint
            ELSE FLOOR(total_count * 0.005)::bigint
        END AS other_non_success_count
    FROM daily_rollup
),
daily_stats AS (
    SELECT
        bucket_start,
        method,
        template_path,
        total_count,
        LEAST(auth_error_count, total_count) AS auth_error_count,
        LEAST(client_error_count, total_count) AS client_error_count,
        LEAST(server_error_count, total_count) AS server_error_count,
        LEAST(other_non_success_count, total_count) AS other_non_success_count
    FROM daily_buckets
),
request_rows AS (
    SELECT
        ds.bucket_start
            + make_interval(hours => ((n - 1) % 24), mins => ((n * 7) % 60), secs => ((n * 13) % 60))
            AS created_at,
        CASE
            WHEN ds.template_path = '/api/v1/public/articles'
                THEN '/api/v1/public/articles?page=' || (((n - 1) % 5) + 1)
            WHEN ds.template_path = '/api/v1/public/articles/{id}'
                THEN '/api/v1/public/articles/' || (1000 + ((n - 1) % 400))
            WHEN ds.template_path = '/api/v1/public/users/{userId}/profile'
                THEN '/api/v1/public/users/' || ((n % 10) + 1) || '/profile'
            WHEN ds.template_path = '/api/v1/public/booking'
                THEN '/api/v1/public/booking'
            WHEN ds.template_path = '/api/v1/public/contact'
                THEN '/api/v1/public/contact'
            WHEN ds.template_path = '/api/v1/auth/login'
                THEN '/api/v1/auth/login'
            WHEN ds.template_path = '/api/v1/public/booking/{id}'
                THEN '/api/v1/public/booking/' || (500 + ((n - 1) % 50))
            ELSE ds.template_path
        END AS path,
        ds.template_path,
        ds.method,
        CASE
            WHEN n <= ds.auth_error_count THEN 401
            WHEN n <= ds.auth_error_count + ds.client_error_count THEN
                CASE WHEN ds.method IN ('POST', 'PUT') THEN 422 ELSE 404 END
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count THEN
                CASE WHEN (n % 3) = 0 THEN 500 ELSE 503 END
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count + ds.other_non_success_count THEN
                429
            ELSE 200
        END AS status,
        (
            CASE
                WHEN ds.method = 'GET' THEN 30
                WHEN ds.method = 'POST' THEN 55
                WHEN ds.method = 'PUT' THEN 70
                ELSE 45
            END
            + (n % 80)
            + CASE
                WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count + ds.other_non_success_count
                    THEN 40
                ELSE 0
              END
        )::bigint AS duration_ms,
        CASE
            WHEN ds.method IN ('POST', 'PUT', 'DELETE')
                THEN CASE (n % 3)
                    WHEN 0 THEN su.user_1
                    WHEN 1 THEN su.user_2
                    ELSE su.user_3
                END
            ELSE NULL
        END AS user_id,
        CASE
            WHEN n <= ds.auth_error_count THEN 'AUTH_UNAUTHORIZED'
            WHEN n <= ds.auth_error_count + ds.client_error_count THEN
                CASE
                    WHEN ds.method IN ('POST', 'PUT') THEN 'VALIDATION_FAILED'
                    ELSE 'RESOURCE_NOT_FOUND'
                END
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count THEN 'INTERNAL_ERROR'
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count + ds.other_non_success_count THEN 'RATE_LIMITED'
            ELSE NULL
        END AS error_code,
        CASE
            WHEN n <= ds.auth_error_count THEN 'Unauthorized access token'
            WHEN n <= ds.auth_error_count + ds.client_error_count THEN
                CASE
                    WHEN ds.method IN ('POST', 'PUT') THEN 'Payload validation failed'
                    ELSE 'Requested resource not found'
                END
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count THEN 'Unexpected server error'
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count + ds.other_non_success_count THEN 'Too many requests'
            ELSE NULL
        END AS error_message,
        CASE
            WHEN ds.method IN ('POST', 'PUT')
                THEN '{"sample":"payload","seedDate":"' || ds.bucket_start || '"}'
            ELSE NULL
        END AS request_body,
        '{"x-request-id":"seed-' || to_char(ds.bucket_start, 'YYYYMMDD') || '-' || n || '"}' AS request_headers,
        '{"content-type":"application/json"}' AS response_headers,
        CASE
            WHEN n <= ds.auth_error_count + ds.client_error_count + ds.server_error_count
                 AND n > ds.auth_error_count + ds.client_error_count
                THEN 'java.lang.RuntimeException: seeded error at ' || ds.template_path
            ELSE NULL
        END AS stack_trace
    FROM daily_stats ds
    CROSS JOIN seed_users su
    CROSS JOIN LATERAL generate_series(1, ds.total_count::int) AS g(n)
)
INSERT INTO request_log (
    path,
    template_path,
    method,
    status,
    duration_ms,
    user_id,
    created_at,
    error_code,
    error_message,
    request_body,
    request_headers,
    response_headers,
    stack_trace
)
SELECT
    path,
    template_path,
    method,
    status,
    duration_ms,
    user_id,
    created_at,
    error_code,
    error_message,
    request_body,
    request_headers,
    response_headers,
    stack_trace
FROM request_rows
ORDER BY created_at, method, template_path;

WITH
date_span AS (
    SELECT gs::date AS bucket_start
    FROM generate_series(DATE '2025-12-24', DATE '2026-03-24', INTERVAL '1 day') gs
),
endpoints AS (
    SELECT *
    FROM (
        VALUES
            ('GET'::varchar(16),    '/api/v1/public/articles'::varchar(2048),                90, 1),
            ('GET'::varchar(16),    '/api/v1/public/articles/{id}'::varchar(2048),           72, 2),
            ('GET'::varchar(16),    '/api/v1/public/users/{userId}/profile'::varchar(2048),  40, 3),
            ('POST'::varchar(16),   '/api/v1/public/booking'::varchar(2048),                  32, 4),
            ('POST'::varchar(16),   '/api/v1/public/contact'::varchar(2048),                  26, 5),
            ('POST'::varchar(16),   '/api/v1/auth/login'::varchar(2048),                      56, 7),
            ('PUT'::varchar(16),    '/api/v1/public/users/{userId}/profile'::varchar(2048),  20, 6),
            ('DELETE'::varchar(16), '/api/v1/public/booking/{id}'::varchar(2048),             14, 8)
    ) AS e(method, template_path, base_traffic, severity)
),
daily_rollup AS (
    SELECT
        d.bucket_start,
        e.method,
        e.template_path,
        GREATEST(
            6,
            (
                e.base_traffic
                + CASE WHEN EXTRACT(ISODOW FROM d.bucket_start) IN (6, 7) THEN -12 ELSE 9 END
                + CASE WHEN EXTRACT(DAY FROM d.bucket_start) BETWEEN 1 AND 5 THEN 7 ELSE 0 END
                + ((EXTRACT(DOY FROM d.bucket_start)::int + e.severity * 3) % 11) - 5
            )
        )::bigint AS total_count,
        e.severity
    FROM date_span d
    CROSS JOIN endpoints e
),
daily_stats AS (
    SELECT
        bucket_start,
        method,
        template_path,
        total_count,
        CASE
            WHEN template_path = '/api/v1/auth/login'
                THEN GREATEST(1, FLOOR(total_count * (0.14 + ((EXTRACT(DAY FROM bucket_start)::int % 5) * 0.01)))::bigint)
            ELSE FLOOR(total_count * (0.01 + (severity * 0.001)))::bigint
        END AS auth_error_count,
        CASE
            WHEN method IN ('POST', 'PUT', 'DELETE')
                THEN FLOOR(total_count * (0.10 + (severity * 0.004)))::bigint
            ELSE FLOOR(total_count * (0.05 + (severity * 0.002)))::bigint
        END AS client_error_count,
        CASE
            WHEN method IN ('POST', 'PUT', 'DELETE')
                THEN FLOOR(total_count * (0.06 + ((EXTRACT(DAY FROM bucket_start)::int % 7) * 0.003)))::bigint
            ELSE FLOOR(total_count * (0.03 + ((EXTRACT(DAY FROM bucket_start)::int % 9) * 0.002)))::bigint
        END AS server_error_count,
        CASE
            WHEN template_path = '/api/v1/auth/login'
                THEN FLOOR(total_count * 0.01)::bigint
            ELSE FLOOR(total_count * 0.005)::bigint
        END AS other_non_success_count
    FROM daily_rollup
)
INSERT INTO endpoint_stats (
    bucket_start,
    method,
    template_path,
    total_count,
    success_count,
    auth_error_count,
    client_error_count,
    server_error_count,
    other_non_success_count
)
SELECT
    bucket_start,
    method,
    template_path,
    total_count,
    GREATEST(
        0,
        total_count - auth_error_count - client_error_count - server_error_count - other_non_success_count
    ) AS success_count,
    auth_error_count,
    client_error_count,
    server_error_count,
    other_non_success_count
FROM daily_stats
ON CONFLICT (bucket_start, method, template_path) DO UPDATE
SET total_count = EXCLUDED.total_count,
    success_count = EXCLUDED.success_count,
    auth_error_count = EXCLUDED.auth_error_count,
    client_error_count = EXCLUDED.client_error_count,
    server_error_count = EXCLUDED.server_error_count,
    other_non_success_count = EXCLUDED.other_non_success_count;

COMMIT;
