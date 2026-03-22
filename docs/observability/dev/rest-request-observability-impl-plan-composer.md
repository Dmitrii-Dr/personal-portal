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
- Async writer: `observability.request-log.async-core-pool-size`, `async-max-pool-size`, `async-queue-capacity` (and reject policy if not fixed in code).
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
| `user_id` | `BIGINT` | nullable; internal numeric id only |
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

Single primary rollup table, e.g. `endpoint_request_stats_daily` or shorter `endpoint_stats` (name is implementation choice; **keep stable once chosen**).

**Naming:** e.g. `V{next}__create_endpoint_request_stats_daily.sql` (or match chosen table name).

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
- `sum_duration_ms` BIGINT
- `max_duration_ms` BIGINT

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
    private long sumDurationMs;
    private long maxDurationMs;
}
```

**Note:** If using composite natural key, align `@IdClass` / `@EmbeddedId` with the unique constraint.

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

**Suggested implementation class:** `DefaultRequestLoggingPathPolicy` with injected lists/suffixes from properties.

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

### Step C2 — Error enrichment hook (exception handler integration)

Global errors should populate `errorCode`, `errorMessage`, `stackTrace` on the same logical record. Prefer **request attributes** set by `@RestControllerAdvice` and read by the filter after response commit.

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

**Coverage note:** failures that never reach `@RestControllerAdvice` (e.g. Spring Security 401/403, some filter errors) will have null `errorCode` / `errorMessage` / `stackTrace`; the row should still be written with the final HTTP status — acceptable per design.

**Coordination task:** enumerate `GlobalExceptionHandler` branches and ensure each mapped error sets code/message consistently (inject the thin `RequestLogErrorContext` or write the same request attributes the filter reads).

---

### Step C3 — Authenticated user id extraction

```java
public interface RequestLogUserIdResolver {
    Long resolveUserId(HttpServletRequest request);
}
```

**Implementation:** read `SecurityContext` / JWT principal → internal numeric `userId` only; return `null` when anonymous.

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
    Long userId,
    Instant createdAt,
    String errorCode,
    String errorMessage,
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

---

### Step C6 — Writer service (entity mapping + save)

```java
public interface RequestLogWriter {
    void persist(RequestLogRecord record);
}
```

**Responsibilities:**

- Map `RequestLogRecord` → `RequestLogEntity`
- Set `createdAt` to UTC clock
- Invoke `RequestLogRepository.save` (or batch later)

**Deliverable:** one row per eligible completed request under integration test conditions (Phase F).

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

**Do not** persist query strings (design §17).

---

### Step C8 — Spring registration

- Declare `RequestLoggingFilter` as a `@Bean` with `FilterRegistrationBean` **or** register via `SecurityFilterChain` if that is the project norm.
- Ensure `/actuator/**` bypasses logging by policy (not only by security).

---

## Phase D — Scheduled maintenance (retention + rollup)

### Step D1 — Retention purge job

**Scheduler:** `@Scheduled` with cron from A1 (default daily **02:00 UTC**); always set `zone = "UTC"` (or equivalent) so cutoffs match operator docs.

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

**Optional schema shortcut:** add generated column or persisted enum `outcome_class` in a follow-up migration if status-based deletes become awkward — only if needed.

---

### Step D2 — Rollup / aggregate job (idempotent)

**Service:**

```java
public interface EndpointRequestStatsRollupService {
    void rollUpSinceLastCheckpoint();
}
```

**Idempotency strategies (pick one):**

- **High-water mark** table `observability_rollup_checkpoint(last_request_log_id)` or reuse a row in a generic `app_checkpoint` table.
- Or **upsert** counts using deterministic keys `(bucket_start, method, template_path)` and **reprocess** a window with transactional boundaries (heavier).

**Processing window:** e.g. hourly batch of new `RequestLogEntity` rows since last id; for each row, classify bucket, update or insert aggregate row:

- Increment counters
- `sum_duration_ms += duration`
- `max_duration_ms = max(max, duration)`

**Schedule:** default **hourly** (rollup cron from A1, e.g. top of each hour UTC); daily is acceptable if design allows more lag — document lag vs real-time dashboards.

**Deliverable:** invariant — for a synthetic batch, sums match classifications.

---

### Step D3 — `@EnableScheduling` and properties

Wire cron/fixed delays via A1 properties; guard with a feature flag property if desired for staged rollout. Keep `@EnableScheduling` / `@EnableAsync` co-located with observability configuration (see A1) so jobs and the writer start together.

---

## Phase E — Read-only admin API

### Step E1 — Query object + specifications

```java
public record RequestLogQuery(
    Instant from,
    Instant to,
    Integer status,
    String templatePath,
    String method,
    Long userId,
    String errorCodeContains,
    String errorMessageContains,
    Pageable pageable
) {}
```

**Alternative query shape:** bind **inclusive calendar days** as `LocalDate dateFrom` / `dateTo` in a separate params object and convert to `Instant` range (`dateFrom` at start of day UTC through `dateTo+1` start exclusive) — matches operator mental model.

```java
public interface RequestLogQueryService {
    Page<RequestLogEntity> search(RequestLogQuery query); // map to list DTOs in service or controller
    Optional<RequestLogEntity> findById(long id);       // for detail endpoint + 404
}
```

**Note:** for text search, prefer `LIKE` with limits or full-text later; enforce max time range and max page size (e.g. cap at 100) to protect DB.

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
    Long userId,
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
    Long userId,
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
@PreAuthorize("hasRole('ADMIN')") // example — align with project admin security
public class RequestLogAdminController {

    @GetMapping
    public Page<RequestLogListItemResponse> list(/* query params → RequestLogQuery */);

    @GetMapping("/{id}")
    public RequestLogDetailResponse detail(@PathVariable long id);
}
```

**Query params:** map to `RequestLogQuery` (or day-based params) in the controller; delegate to `RequestLogQueryService` and mappers. Return **404** when `id` missing.

**Authorization:** admin-only — method- or class-level security consistent with other admin controllers.

**Path naming:** singular `request-log` vs plural `request-logs` is a product choice; stay consistent with existing admin URLs.

---

### Step E4 — Optional aggregates read endpoint

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
    long otherNonSuccessCount,
    long sumDurationMs,
    long maxDurationMs
) {}
```

Expose read-only listing with pagination or capped result set. **Never** return JPA entities from these endpoints — use `EndpointStatsDailyResponse` (or equivalent) only.

---

## Phase F — Testing and verification (ordered)

### Unit tests

| Component | Scenarios |
|-----------|-----------|
| `HttpOutcomeClassifier` | See B1 matrix; include edge codes (204, 301, 404, 503). |
| `RequestLogOutcomeClassifier` | 2xx → `SUCCESS`; 401 → `FAILURE`; 500 → `FAILURE`. |
| `StackTraceTruncator` | null → null; short → unchanged; exactly `MAX_BYTES` → unchanged; over cap → truncated with marker. |
| `RequestLoggingPathPolicy` | `/actuator/health` → no capture; `/admin/...` 200 → skip success; `/admin/...` 5xx → capture; `/api/v1/admin/...` 200 → capture if policy only targets `/admin/**` for embedded admin. |
| `TemplatePathResolver` | attribute present → pattern; absent → `UNKNOWN`. |
| Entity mapping (`RequestLogRecord` / event → entity) | All fields preserved; timestamps UTC. |
| Rollup mapping | Each `HttpOutcomeBucket` increments the correct aggregate counter on `EndpointRequestStatsDailyEntity`. |

### Integration tests

| Scenario | Expected |
|----------|----------|
| Authenticated `POST /api/v1/auth/login` → 200 | One `request_log` row; `status=200`; error fields null. |
| Same login → 401 | One row; `status=401`; error fields may be null if not from advice. |
| `GET /actuator/health` | Zero rows. |
| `GET /admin/...` → 200 (per policy) | Zero rows. |
| `GET /admin/...` → 500 | One row; `status=500`. |
| `GET /api/v1/nonexistent` → 404 | One row; `templatePath` sentinel (e.g. `UNKNOWN`). |
| Controller-mapped exception → 4xx/422 with body | One row; non-null `errorCode` / `errorMessage` / truncated `stackTrace` when advice wired. |
| Retention job | Success rows older than success window deleted; failures older than failure window deleted; recent rows and aggregate table untouched. |
| Rollup job | Counts match classified rows for a synthetic day; second run **idempotent** (same totals). |
| Admin API without admin role | 403. |
| Admin list with `status=500` | Only matching rows; list DTOs **without** `stackTrace`. |
| Admin `GET .../request-logs/{id}` | Detail DTO **with** `stackTrace`; unknown id → 404. |

Run filter/stack tests with **WebMvcTest / MockMvc** or **`@SpringBootTest`**, matching project norms.

---

## Phase G — Operations documentation (short, required)

- Document **UTC** usage for `created_at`, `bucket_start`, and retention cutoff interpretation (design §15 item 6).
- Document async loss semantics and monitoring suggestions (queue depth, rejected tasks, DB errors).
- Document admin API auth requirements.

---

## Suggested module / package layout (non-binding)

- `...observability.config` — properties, `@EnableAsync` / `@EnableScheduling`, executor beans, filter registration
- `...observability.capture` — filter, context, resolvers, record factory
- `...observability.classification` or `...observability.capture` — `HttpOutcomeClassifier`, `RequestLogOutcomeClassifier`, `StackTraceTruncator`, path policy
- `...observability.persistence` — entities, repos, writer
- `...observability.jobs` — retention, rollup
- `...observability.admin` — controller, DTOs, query service

Adjust names to match existing project conventions.

---

## Dependency graph (summary)

```
A1 properties → C5/C7/D*
A2–A5 schema + repos → C6, D1–D2, E1
B* classifiers/policy + B2a truncator → C2, C7, D2
C1–C6 + C7–C8 capture → produces rows
D1 retention (needs A2, A5, B2)
D2 rollup (needs A3, A5, B1, C6)
E* admin API (needs A5, authorization infra)
F tests parallelize after B*, C*, D*, E* pieces exist
```

---

## Checklist mapping to design rollout §15

| Design §15 item | Plan steps |
|-----------------|------------|
| Flyway `request_log` + indexes | A2, A4, A5 |
| Flyway aggregate + unique constraint | A3, A4, A5 |
| Capture filter + async writer | C1–C8, A1 |
| Error fields from handler | C2 + B2a + exception handler touchpoints |
| Retention 7d / 30d | D1, A1 |
| Rollup daily / indefinite aggregates | D2–D3, A1 |
| Prefix rules | B3, C7 |
| Read-only admin API | E1–E3 (list + detail DTOs); E4 aggregates optional |
| UTC documentation | G |

This sequence is intended so that **schema and pure rules land first**, **persistence path second**, **background jobs third**, and **operator-facing API last**, minimizing rework and keeping tests meaningful at each step.
