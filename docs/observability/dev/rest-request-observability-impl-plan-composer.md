# REST request observability — implementation plan

This document turns [REST request observability — high-level design](../design/rest-request-observability.md) into an **ordered, step-by-step implementation sequence**. It may name Java types and show **signatures and field shapes** only; it does **not** contain full implementations.

---

## 0. References and constraints

- **Source of truth:** `docs/observability/design/rest-request-observability.md` (locked decisions in §2, rollout checklist §15).
- **Runtime:** single Spring Boot app module `personal-portal-application`, PostgreSQL, Flyway, JPA where appropriate.
- **Project rules:** controllers remain thin; **never** return JPA entities from HTTP APIs — use response DTOs for the read-only observability admin API.
- **Durability:** best-effort async persistence; bounded loss on crash is acceptable (no transactional outbox in v1).

Implement in the order below unless a step explicitly depends on a later one (dependencies are called out).

---

## Phase A — Foundation (config, schema, domain skeleton)

### Step A1 — Configuration properties

Add a `@ConfigurationProperties` prefix (or equivalent binding) for observability tuning, aligned with the design:

- Retention windows: `observability.request-log.retention-success-days`, `observability.request-log.retention-failure-days`.
- Job schedules: `observability.request-log.retention-cron`, `observability.request-log.rollup-cron` (use explicit **zone**, e.g. `UTC`, on `@Scheduled`).
- Async writer: `observability.request-log.async-core-pool-size`, `async-max-pool-size`, `async-queue-capacity`,
  plus flush controls `async-flush-batch-size` and `async-flush-interval-ms` (and reject policy if not fixed in code).
- Optional: static-resource detection toggles; stack-trace max length should stay in sync with `StackTraceTruncator` (Phase B2a).

**Deliverable:** bound properties class + documented defaults in `application.properties` (or profile-specific files). Enable async/scheduling on the configuration class that declares the executor bean (e.g. `@EnableAsync`, `@EnableScheduling`).

**Suggested type (signature-level only):**

```java
@ConfigurationProperties(prefix = "observability.request-log")
public class RequestLogObservabilityProperties {
    int retentionSuccessDays = 7;
    int retentionFailureDays = 30;
    String retentionCron = "0 0 2 * * *";   // daily 02:00 UTC — override via properties
    String rollupCron = "0 0 * * * *";      // hourly top of hour UTC — override via properties
    int asyncCorePoolSize = 2;
    int asyncMaxPoolSize = 4;
    int asyncQueueCapacity = 1000;
    int asyncFlushBatchSize = 100;
    long asyncFlushIntervalMs = 1000;
}
```

**Example `application.properties` keys (defaults):**

```properties
observability.request-log.retention-success-days=7
observability.request-log.retention-failure-days=30
observability.request-log.retention-cron=0 0 2 * * *
observability.request-log.rollup-cron=0 0 * * * *
observability.request-log.async-core-pool-size=2
observability.request-log.async-max-pool-size=4
observability.request-log.async-queue-capacity=1000
observability.request-log.async-flush-batch-size=100
observability.request-log.async-flush-interval-ms=1000
```

---

### Step A2 — Flyway: `request_log` table

Create a versioned migration **before** any code that writes rows.

**Naming:** e.g. `V{next}__create_request_log.sql` (use the project’s next Flyway version).

**Columns** (map to JPA entity in A4; DB names snake_case):

| Column | SQL type | Notes |
|--------|----------|--------|
| `id` | `BIGSERIAL PRIMARY KEY` | |
| `path` | `VARCHAR(2048) NOT NULL` | raw path, **no query string** |
| `template_path` | `VARCHAR(2048) NOT NULL` | MVC best-matching pattern or sentinel `UNKNOWN` |
| `method` | `VARCHAR(16) NOT NULL` | HTTP method |
| `status` | `INTEGER NOT NULL` | final HTTP status |
| `duration_ms` | `BIGINT NOT NULL` | wall-clock latency |
| `user_id` | `UUID` | nullable; internal user uuid id only |
| `created_at` | `TIMESTAMPTZ NOT NULL` | store **UTC**; document for operators |
| `error_code` | `VARCHAR(128)` | nullable |
| `error_message` | `TEXT` | nullable |
| `stack_trace` | `TEXT` | nullable; cap length in app (~64 KB + marker; see B2a) |

**Indexes** (per design §15; same migration or a follow-up before go-live):

- `(created_at)` — retention scans
- `(template_path, created_at)` — admin filters and rollup high-water queries
- `(status, created_at)` — admin filters

**Deliverable:** single Flyway script applying table + indexes; verify on clean DB.

---

### Step A3 — Flyway: daily aggregate table

Single primary rollup table, `endpoint_stats` .

**Naming:** e.g. `V{next}__create_endpoint_stats.sql`.

**Logical key:** `(bucket_start, method, template_path)` with a **UNIQUE** constraint.

**Columns** (minimum):

- `bucket_start` DATE (UTC calendar day per locked decision; document boundary)
- `method` VARCHAR
- `template_path` VARCHAR
- `total_count` BIGINT
- `success_count` BIGINT
- `auth_error_count` BIGINT (401 + 403)
- `client_error_count` BIGINT (other 4xx)
- `server_error_count` BIGINT (5xx)
- `other_non_success_count` BIGINT (3xx, 1xx, odd codes per design)

**Deliverable:** Flyway migration + unique constraint; optional supporting index on `(bucket_start, template_path)` for dashboard queries.

---

### Step A4 — JPA entities (minimal annotations)

Define entities mirroring A2–A3. Keep packages consistent with existing modules (e.g. `...observability.persistence` or `...model`).

**Suggested entities (fields only, no logic):**

```java
@Entity
@Table(name = "request_log")
public class RequestLogEntity {
    private Long id;
    private String path;
    private String templatePath;
    private String method;
    private Integer status;
    private Long durationMs;
    private Long userId;
    private Instant createdAt; // or OffsetDateTime — pick one project standard
    private String errorCode;
    private String errorMessage;
    private String stackTrace;
}
```

```java
@Entity
@Table(name = "endpoint_request_stats_daily")
public class EndpointRequestStatsDailyEntity {
    private Long id; // surrogate optional if you prefer composite @Id
    private LocalDate bucketStart;
    private String method;
    private String templatePath;
    private long totalCount;
    private long successCount;
    private long authErrorCount;
    private long clientErrorCount;
    private long serverErrorCount;
    private long otherNonSuccessCount;
}
```

---

### Step A5 — Spring Data repositories

**Interfaces:**

```java
public interface RequestLogRepository extends JpaRepository<RequestLogEntity, Long>, JpaSpecificationExecutor<RequestLogEntity> {

    /** Success rows: 2xx — delete older than cutoff (see D1). */
    @Modifying
    @Query("DELETE FROM RequestLogEntity r WHERE r.status >= 200 AND r.status < 300 AND r.createdAt < :cutoff")
    int deleteSuccessRowsOlderThan(@Param("cutoff") Instant cutoff);

    /** Failure rows: non-2xx — delete older than cutoff. */
    @Modifying
    @Query("DELETE FROM RequestLogEntity r WHERE (r.status < 200 OR r.status >= 300) AND r.createdAt < :cutoff")
    int deleteFailureRowsOlderThan(@Param("cutoff") Instant cutoff);

    /** Optional: window fetch for rollup-by-day strategies that reprocess a closed UTC day. */
    List<RequestLogEntity> findByCreatedAtBetween(Instant from, Instant to);
}
```

Adjust JPQL if the entity or `createdAt` type differs; Criteria API is an equivalent option. If deletes-by-status become awkward, add a denormalized `outcome_class` column (see D1).

```java
public interface EndpointRequestStatsDailyRepository extends JpaRepository<EndpointRequestStatsDailyEntity, Long> {
    Optional<EndpointRequestStatsDailyEntity> findByBucketStartAndMethodAndTemplatePath(...);
    // Dashboard range queries as needed
}
```

**Deliverable:** repositories compile; optional `JpaSpecificationExecutor` for admin list filters.

---

## Phase B — Pure classification and routing rules (test-first)

These components have **no servlet dependencies** so they can be unit-tested early.

Implementation status (`personal-portal-admin`):
- Implemented `HttpOutcomeBucket`, `HttpOutcomeClassifier`, `RequestLogOutcomeClass`, `RequestLogOutcomeClassifier`.
- Implemented `StackTraceTruncator` (`MAX_BYTES = 65_536`, marker `\n... [truncated]`).
- Implemented `RequestLoggingPathPolicy` + `DefaultRequestLoggingPathPolicy` using internal default path/suffix lists.
- Added `TemplatePathResolver` contract with sentinel constant `UNKNOWN`.
- Added unit tests for classifier matrix, success predicate, outcome class mapping, stack-trace truncation boundaries, and policy matrix (including `/admin/**` success-skip vs error capture behavior).

### Step B1 — HTTP outcome classification

Centralize mapping from `int status` to aggregate buckets per design §8.

**Suggested API:**

```java
public enum HttpOutcomeBucket {
    SUCCESS_2XX,
    AUTH_ERROR_401_403,
    CLIENT_ERROR_4XX_OTHER,
    SERVER_ERROR_5XX,
    OTHER_NON_SUCCESS
}

public final class HttpOutcomeClassifier {
    public static HttpOutcomeBucket classify(int httpStatus);
    public static boolean isSuccess(int httpStatus); // true iff 2xx
}
```

**Invariant tests:** 2xx → success; 401/403 → auth; other 4xx → client; 5xx → server; 3xx → other_non_success.

**Suggested unit matrix (status → bucket):**

| HTTP status | Expected `HttpOutcomeBucket` |
|-------------|-----------------------------|
| 200, 204 | `SUCCESS_2XX` |
| 301 | `OTHER_NON_SUCCESS` |
| 400, 404 | `CLIENT_ERROR_4XX_OTHER` |
| 401, 403 | `AUTH_ERROR_401_403` |
| 500, 503 | `SERVER_ERROR_5XX` |

Also assert `isSuccess` for representative 2xx / non-2xx codes.

---

### Step B2 — Detail retention class (success vs failure row)

For purge jobs and optional column denormalization:

```java
public enum RequestLogOutcomeClass {
    SUCCESS,
    FAILURE
}

public final class RequestLogOutcomeClassifier {
    public static RequestLogOutcomeClass fromHttpStatus(int httpStatus);
}
```

**Rule:** failure class = anything not 2xx (matches design §8 retention).

---

### Step B2a — Stack trace truncation (`StackTraceTruncator`)

Pure utility used when building error fields (Phase C2 / record factory). Keeps DB row size bounded and matches design §2 / §7.

**Suggested API:**

```java
public final class StackTraceTruncator {

    public static final int MAX_BYTES = 65_536; // 64 KB UTF-8 encoded length cap
    public static final String TRUNCATION_MARKER = "\n... [truncated]";

    /** Truncates to MAX_BYTES UTF-8 octets; appends TRUNCATION_MARKER when truncated. Null in → null out. */
    public static String truncate(String stackTrace);
}
```

**Unit tests:** null → null; shorter than cap → unchanged; exactly `MAX_BYTES` → unchanged; `MAX_BYTES + 1` → truncated with marker.

---

### Step B3 — Path prefix and static skip rules

Implement decision table from design §2.1, §12:

```java
public interface RequestLoggingPathPolicy {
    boolean shouldCaptureAtAll(String path);           // false for /actuator/**
    boolean shouldSkipSuccess(String path);            // true for /admin/** success only
    boolean isProbablyStaticAsset(String path, Object handlerOrNull); // optional ResourceHandler check
}
```

**Suggested implementation class:** `DefaultRequestLoggingPathPolicy` with internal defaults (or injected lists/suffixes from properties if runtime configurability is needed).

**Tests:** matrix for `/api/v1/...`, `/admin/...`, `/actuator/...`, static-like paths. Include **admin success vs error** explicitly, e.g. `/admin/sba/...` with 200 → no capture (when policy says success skip); same path with 5xx → capture. Confirm `/api/v1/admin/...` is **not** treated as Spring Boot Admin UI path if your policy only skips `/admin/**` for embedded admin (align with design §2.1).

---

### Step B4 — Template path resolution contract

Abstract how `templatePath` is obtained from the request (dispatcher attributes) vs sentinel.

```java
public interface TemplatePathResolver {
    String resolveTemplatePath(HttpServletRequest request);
}
```

**Constants:** e.g. `UNKNOWN_TEMPLATE_PATH = "UNKNOWN"` (agreed sentinel).

**Implementation notes (later wiring):** use `HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE` when present; for unmatched dispatches, return sentinel (design §5, §9).

---

## Phase C — Capture pipeline (servlet filter + async writer)

Implementation status (`personal-portal-admin`):
- Implemented `RequestLogCaptureContext` request-scoped holder with `attach(...)` / `current(...)` and `startedAt`.
- Added `RequestLogAttributes` constants for `CAPTURE_CONTEXT`, `ERROR_CODE`, `ERROR_MESSAGE`, `STACK_TRACE`.
- Implemented thin error enrichment contract `RequestLogErrorContext` with request-attribute-based implementation `RequestAttributeRequestLogErrorContext`.
- `RequestAttributeRequestLogErrorContext` truncates stack traces via `StackTraceTruncator.truncate(...)` before storing values.
- Added `ObservabilityErrorAttributesExceptionResolver` (highest precedence, non-terminal) that records request-log error attributes for MVC exceptions before normal handler resolution continues.
- Implemented `RequestLogUserIdResolver` as `SecurityContextRequestLogUserIdResolver` returning authenticated `UUID` user id (or `null` when anonymous/malformed).
- Added unit tests for context attachment/readout and error enrichment attribute propagation/truncation behavior.

### Step C1 — Request-scoped capture context

Store **start time** and any **pre-response** fields without blocking the response.

**Suggested holder (request attribute or thread-local cleared in filter finally — prefer request attributes):**

```java
public final class RequestLogCaptureContext {
    public static void attach(HttpServletRequest request, Instant startedAt);
    public static Optional<RequestLogCaptureContext> current(HttpServletRequest request);
    Instant getStartedAt();
    // Optional: mutable slots for error fields set from advice
}
```

---

### Step C2 — Error enrichment hook (exception resolver + advice integration)

Global errors should populate `errorCode`, `errorMessage`, `stackTrace` on the same logical record. Use **request attributes** populated by the exception path and read by the filter after response commit.

**Suggested attribute keys:** e.g. `RequestLogAttributes.ERROR_CODE`, etc.

**Interface for advice to call (thin):**

```java
public interface RequestLogErrorContext {
    void recordApiError(HttpServletRequest request, String errorCode, String message, Throwable cause);
}
```

**Implementation responsibilities:**

- Truncate via `StackTraceTruncator.truncate(...)` (B2a) before the value lands on `RequestLogRecord` / persistence.
- Do not store PII in `message` beyond what existing API error bodies already expose (design §5).

**Implemented fallback:** `ObservabilityErrorAttributesExceptionResolver` now records `errorMessage` + `stackTrace` for any MVC exception (for example argument binding/conversion failures) and returns `null` so the regular resolver chain still produces the API response. This removes the previous gap where stack traces were logged but not persisted.

**Coverage note:** failures that never reach Spring MVC exception resolution (e.g. some Security 401/403 paths, low-level filter errors) can still have null `errorCode` / `errorMessage` / `stackTrace`; the row should still be written with the final HTTP status — acceptable per design.

**Coordination task (remaining):** enumerate `GlobalExceptionHandler` branches and ensure each mapped error sets canonical `errorCode` consistently (fallback resolver guarantees stack trace capture but uses `null` error code by default).

---

### Step C3 — Authenticated user id extraction

```java
public interface RequestLogUserIdResolver {
    UUID resolveUserId(HttpServletRequest request);
}
```

**Implementation:** read `SecurityContext` / JWT principal → internal `UUID userId`; return `null` when anonymous.

---

### Step C4 — Build `RequestLog` write model (non-entity DTO)

Keep filter independent of JPA:

```java
public record RequestLogRecord(
    String path,
    String templatePath,
    String method,
    int status,
    long durationMs,
    UUID userId,
    Instant createdAt,
    String errorCode,
    String errorMessage,
    String requestBody, // sanitized JSON only; sensitive fields masked
    String requestHeaders, // sanitized headers JSON; sensitive values redacted
    String responseHeaders, // sanitized headers JSON; sensitive values redacted
    String stackTrace
) {}
```

```java
public interface RequestLogRecordFactory {
    RequestLogRecord build(HttpServletRequest request, HttpServletResponse response, RequestLogCaptureContext ctx);
}
```

---

### Step C5 — Async persistence gateway

**Interface:**

```java
public interface RequestLogPersistenceGateway {
    void enqueue(RequestLogRecord record);
}
```

**Implementation choices (pick one for v1):**

- **Recommended default:** a named `ThreadPoolTaskExecutor` bean (e.g. `observabilityExecutor`) with bounded queue, **core / max / queue** from A1. Under load, a **discard** policy (e.g. `DiscardPolicy` or `DiscardOldestPolicy`) matches “best-effort loss” — document the choice. Optionally drive persistence with `@Async("observabilityExecutor")` on an internal `persist` method while `enqueue` returns immediately.
- Alternative: in-memory queue consumed by a single worker thread.

**Bean lifecycle:** graceful shutdown: attempt to drain or log dropped backlog size (best-effort).

**Current implementation (personal-portal-admin):**

- `DefaultRequestLogRecordFactory` builds `RequestLogRecord` from servlet request/response + `RequestLogCaptureContext`.
- `AsyncRequestLogPersistenceGateway` buffers request logs in memory and flushes batches to `RequestLogWriter`
  when either flush threshold is reached:
  - batch size threshold (`observability.request-log.async-flush-batch-size`)
  - max buffered time threshold (`observability.request-log.async-flush-interval-ms`)
- Executor rejection strategy is `ThreadPoolExecutor.DiscardPolicy` (best-effort drop under pressure).
- Unexpected service failure/restart may lose queued in-memory log records by design (accepted for v1).

---

### Step C6 — Writer service (entity mapping + save)

```java
public interface RequestLogWriter {
    void persistBatch(List<RequestLogRecord> records);
}
```

**Responsibilities:**

- Map `RequestLogRecord` → `RequestLogEntity`
- Set `createdAt` to UTC clock
- Invoke batched persistence (`RequestLogRepository.saveAll`) for flush batches

**Deliverable:** one row per eligible completed request under integration test conditions (Phase F).

**Current implementation (personal-portal-admin):**

- Implemented `RequestLogWriter` with `RepositoryRequestLogWriter`.
- `RepositoryRequestLogWriter` maps `RequestLogRecord` to `RequestLogEntity`.
- `createdAt` is assigned from injected UTC clock (`Clock.systemUTC()` bean).
- Persist path uses `RequestLogRepository.saveAll(...)` on flushed batches.

---

### Step C7 — Servlet filter (ordering + after-response)

```java
public class RequestLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain);
}
```

**Ordering:** register early enough to measure full chain; still **after** security filters if `userId` must be populated — **document** chosen `Filter` order relative to Spring Security. If `userId` is unavailable early, resolve it **after** chain in `finally` using the same request object.

**Algorithm (high level):**

1. If `!pathPolicy.shouldCaptureAtAll(path)` → `chain.doFilter` and return.
2. Attach `RequestLogCaptureContext` with start instant.
3. `try { chain.doFilter } finally {`
4. Compute `durationMs`, read `status`, resolve `templatePath`, `userId`, merge error attributes.
5. If `/admin/**` and success → skip enqueue.
6. If static skip → skip enqueue.
7. Build `RequestLogRecord` → `persistenceGateway.enqueue`.

**Do not** persist query strings (design §17). Persist request body only after sanitization/masking.

**Current implementation (personal-portal-admin):**

- Added `RequestLoggingFilter` (`OncePerRequestFilter`) that:
  - bypasses non-captured paths via `RequestLoggingPathPolicy.shouldCaptureAtAll(...)`,
  - attaches `RequestLogCaptureContext` before `chain.doFilter(...)`,
  - finalizes in `finally` (after response) and merges request error attributes into capture context,
  - skips `/admin/**` successful responses via `shouldSkipSuccess(...)` + `HttpOutcomeClassifier.isSuccess(...)`,
  - skips static assets via `isProbablyStaticAsset(...)`,
  - builds `RequestLogRecord` using `RequestLogRecordFactory` and enqueues via `RequestLogPersistenceGateway`.
- Uses `request.getRequestURI()` for persisted path input, so query strings are not persisted.

---

### Step C8 — Spring registration

- Register `RequestLoggingFilter` as a Spring bean (component or `@Bean`) and wire a `FilterRegistrationBean` **or** register via `SecurityFilterChain` if that is the project norm.
- Ensure `/actuator/**` bypasses logging by policy (not only by security).

**Current implementation (personal-portal-admin):**

- `RequestLoggingFilter` is declared as a component bean and still registered explicitly via `FilterRegistrationBean` in `RequestLogObservabilityAutoConfiguration`.
- Registered `FilterRegistrationBean<RequestLoggingFilter>` with:
  - URL pattern `/*`,
  - explicit order `-90` (documented as after Spring Security delegated filter default `-100`, while still early in the servlet chain).
- `/actuator/**` bypass remains enforced by `RequestLoggingPathPolicy` default no-capture prefixes.

---

## Phase D — Scheduled maintenance (retention + rollup)

### Step D1 — Retention purge job

**Scheduler:** `@Scheduled` with cron from A1 (default daily **02:00 UTC**); always set `zone = "UTC"` (or equivalent) so cutoffs match operator docs.

**Status:** Implemented in `personal-portal-admin` with `RequestLogRetentionScheduler` + `BatchedRequestLogRetentionService`.

**Service:**

```java
public interface RequestLogRetentionService {
    void purgeExpiredDetailRows();
}
```

**Deletion strategy:**

- Delete success-class rows with `created_at < now() - retentionSuccessDays`
- Delete failure-class rows with `created_at < now() - retentionFailureDays`
- **Do not** touch aggregate table

**Implementation:** delegate to `RequestLogRepository` `@Modifying` deletes (A5) with `Instant` cutoffs computed in the service, or equivalent Criteria bulk delete.

**Performance guardrails (implemented):**

- Purge runs in bounded batches (`observability.request-log.retention-delete-batch-size`, default `500`) instead of one large delete.
- Each scheduler execution is capped (`observability.request-log.retention-max-batches-per-run`, default `200`) to avoid sustained DB spikes.
- Repository delete SQL uses `DELETE ... WHERE ctid IN (SELECT ... ORDER BY created_at LIMIT :batchSize)` so each statement remains small and predictable.
- Failure cleanup is split into two status ranges (`status >= 300` and `status < 200`) to avoid a wide `OR` predicate that can degrade index usage.
- Existing index `idx_request_log_status_created_at(status, created_at)` is used for candidate scan order; no aggregate table writes/deletes are performed here.
- No cascade-heavy deletes are triggered by retention: `request_log` references `users` with `ON DELETE SET NULL` (reverse direction), so deleting request-log rows does not fan out.

**Optional schema shortcut:** add generated column or persisted enum `outcome_class` in a follow-up migration if status-based deletes become awkward — only if needed.

---

### Step D2 — Rollup / aggregate job (idempotent)

**Service:**

```java
public interface EndpointRequestStatsRollupService {
    void rollUpSinceLastCheckpoint();
}
```

**Recommended solution (selected): high-water mark checkpoint table**

Use a dedicated checkpoint row (or `app_checkpoint` key) to store one global cursor for this job, e.g.:

- `job_name = 'endpoint_stats_rollup'`
- `last_processed_request_log_id BIGINT NOT NULL`
- `updated_at TIMESTAMP`


**Exactly-once behavior (transactional invariant):**

- Read checkpoint `N`.
- Fetch `request_log` rows with `id > N` ordered by `id` (bounded batch).
- Aggregate deltas in memory by `(bucket_start, method, template_path)` and outcome bucket.
- Upsert all affected daily aggregate rows.
- Update checkpoint to max processed id `M`.
- Commit once.

If the service crashes before commit, both aggregate writes and checkpoint update are rolled back. On restart, job retries from `N` without double counting or data loss.

**Startup/recovery rule:**

- On startup, read checkpoint from DB.
- If no checkpoint exists, initialize once with `last_processed_request_log_id = 0` (or bootstrap value by policy) and continue.
- Continue normal scheduled execution; no special "current period row scan" is required.

**Processing window:** e.g. hourly batch of new `RequestLogEntity` rows since last id; for each row, classify bucket, update or insert aggregate row:

- Increment counters
- Ensure current UTC day contains a row for every known `(method, template_path)` pair. If a pair has no requests today, create a zero-valued row so daily row count remains stable across no-traffic days.

**Schedule:** default **hourly** (rollup cron from A1, e.g. top of each hour UTC);

---

### Step D3 — `@EnableScheduling` and properties

Implemented in `personal-portal-admin`:

- `@EnableScheduling` and `@EnableAsync` are co-located in `RequestLogObservabilityAutoConfiguration`.
- Most observability implementations are now class-level beans (`@Service`/`@Component`) instead of factory methods in auto-configuration.
- Retention and rollup schedulers use cron properties:
  - `observability.request-log.retention-cron`
  - `observability.request-log.rollup-cron`
- Both schedulers run with `zone = "UTC"` on `@Scheduled`.
- Feature flags are wired for staged rollout via `@ConditionalOnProperty` on scheduler classes:
  - `observability.request-log.retention-job-enabled` (default `true`)
  - `observability.request-log.rollup-job-enabled` (default `true`)

Result: jobs and async writer are started by the same observability auto-configuration, and either scheduled job can be disabled without changing code.

---

## Phase E — Read-only admin API

### Step E1 — Query object + specifications

**Decision:** use `Instant from` / `Instant to` as the canonical time filter for Phase E.

```java
public record RequestLogQuery(
    Instant from,
    Instant to,
    Integer status,
    String templatePath,
    String method,
    UUID userId,
    String errorCodeContains,
    String errorMessageContains,
    Pageable pageable
) {}
```


```java
public interface RequestLogQueryService {
    Page<RequestLogEntity> search(RequestLogQuery query); // map to list DTOs in service or controller
    Optional<RequestLogEntity> findById(long id);       // for detail endpoint + 404
}
```

**Note:** for text search, prefer `LIKE` with limits or full-text later; enforce max time range and max page size (cap `size` at 100) to protect DB. Also enforce a deterministic default sort (e.g. `createdAt DESC, id DESC`).

---

### Step E2 — Response DTOs (no entity leak)

Use a **list projection without `stackTrace`** and a **detail projection with `stackTrace`** so list endpoints stay small and do not ship large traces by default.

```java
public record RequestLogListItemResponse(
    long id,
    String path,
    String templatePath,
    String method,
    int status,
    long durationMs,
    UUID userId,
    Instant createdAt,
    String errorCode,
    String errorMessage
) {}
```

```java
public record RequestLogDetailResponse(
    long id,
    String path,
    String templatePath,
    String method,
    int status,
    long durationMs,
    UUID userId,
    Instant createdAt,
    String errorCode,
    String errorMessage,
    String stackTrace
) {}
```

**Optional wrapper** (if not using `Page<RequestLogListItemResponse>` directly): `items`, `totalElements`, `totalPages`, `page`, `size`.

```java
public interface RequestLogAdminMapper {
    RequestLogListItemResponse toListItem(RequestLogEntity entity);
    RequestLogDetailResponse toDetail(RequestLogEntity entity);
}
```

---

### Step E3 — Controller (thin)

```java
@RestController
@RequestMapping("/api/v1/admin/observability/request-logs")
// URL path should stay under /api/v*/admin/** so existing admin security matcher applies.
public class RequestLogAdminController {

    @GetMapping
    public Page<RequestLogListItemResponse> list(/* query params → RequestLogQuery */);

    @GetMapping("/{id}")
    public RequestLogDetailResponse detail(@PathVariable long id);
}
```

**Query params:** map `from` / `to` (+ optional filters) to `RequestLogQuery` in the controller; delegate to `RequestLogQueryService` and mappers. Return **404** when `id` missing.

**Authorization:** admin-only — method- or class-level security consistent with other admin controllers.

**Path naming:** singular `request-log` vs plural `request-logs` is a product choice; stay consistent with existing admin URLs.

---

### Step E4 — Optional aggregates read endpoint

**Implemented endpoint (Phase E):** `GET /api/v1/admin/observability/endpoint-stats`
with optional filters `from` (`LocalDate`), `to` (`LocalDate`), `method`, `templatePath`,
plus pageable params. Page size is capped at `100`; invalid `from > to` returns `400`.

```java
public record EndpointStatsDailyResponse(
    LocalDate bucketStart,
    String method,
    String templatePath,
    long totalCount,
    long successCount,
    long authErrorCount,
    long clientErrorCount,
    long serverErrorCount,
    long otherNonSuccessCount
) {}
```

Expose read-only listing with pagination or capped result set. **Never** return JPA entities from these endpoints — use `EndpointStatsDailyResponse` (or equivalent) only.

---