# REST Request Observability — Implementation Plan

> **Reference design:** `docs/observability/design/rest-request-observability.md`
>
> This document describes **what to build, in what order, and how to structure it**. It includes
> class/interface/method signatures but **no full implementation code**.

---

## Overview

The implementation is broken into **8 sequential phases**. Each phase must be complete before the
next begins, because later phases depend on the artifacts introduced by earlier ones.

```
Phase 1 → DB Schema (Flyway migrations)
Phase 2 → Domain Model (JPA entities + repositories)
Phase 3 → Core Capture Support (enums, DTOs, classifier, async writer)
Phase 4 → Servlet Filter (intercept, time, build, enqueue)
Phase 5 → Exception Handler Integration (error fields population)
Phase 6 → Scheduled Jobs (retention purge + daily rollup)
Phase 7 → Read-only Admin API (list, filter, paginate)
Phase 8 → Configuration + Testing
```

---

## Phase 1 — Database Schema (Flyway Migrations)

**Goal:** create the two tables and their indexes before any Java code references them.

### 1.1 Migration: `request_log` table

File: `V{next}__create_request_log.sql`

Columns:

| Column | SQL type | Notes |
|--------|----------|-------|
| `id` | `BIGSERIAL PRIMARY KEY` | |
| `path` | `VARCHAR(2048) NOT NULL` | raw path, no query string |
| `template_path` | `VARCHAR(2048) NOT NULL` | MVC best-matching pattern or `UNKNOWN` |
| `method` | `VARCHAR(16) NOT NULL` | HTTP method |
| `status` | `INTEGER NOT NULL` | final HTTP status code |
| `duration_ms` | `BIGINT NOT NULL` | wall-clock latency |
| `user_id` | `BIGINT` | nullable; numeric internal id only |
| `created_at` | `TIMESTAMP NOT NULL` | UTC |
| `error_code` | `VARCHAR(128)` | nullable; stable API error code |
| `error_message` | `TEXT` | nullable |
| `stack_trace` | `TEXT` | nullable; capped at 64 KB with truncation marker |

Indexes (same migration or a follow-up migration before going live):

- `(created_at)` — for retention purge scans
- `(template_path, created_at)` — for rollup jobs and admin filters
- `(status, created_at)` — for outcome-based retention classification

### 1.2 Migration: `endpoint_stats` table

File: `V{next+1}__create_endpoint_stats.sql`

Columns:

| Column | SQL type | Notes |
|--------|----------|-------|
| `id` | `BIGSERIAL PRIMARY KEY` | |
| `bucket_start` | `DATE NOT NULL` | UTC calendar day |
| `method` | `VARCHAR(16) NOT NULL` | |
| `template_path` | `VARCHAR(2048) NOT NULL` | |
| `total_count` | `BIGINT NOT NULL DEFAULT 0` | |
| `success_count` | `BIGINT NOT NULL DEFAULT 0` | 2xx |
| `auth_error_count` | `BIGINT NOT NULL DEFAULT 0` | 401 + 403 combined |
| `client_error_count` | `BIGINT NOT NULL DEFAULT 0` | other 4xx |
| `server_error_count` | `BIGINT NOT NULL DEFAULT 0` | 5xx |
| `other_non_success_count` | `BIGINT NOT NULL DEFAULT 0` | 3xx and other non-classified |
| `sum_duration_ms` | `BIGINT NOT NULL DEFAULT 0` | for average latency queries |
| `max_duration_ms` | `BIGINT NOT NULL DEFAULT 0` | |

Constraints:

- `UNIQUE (bucket_start, method, template_path)` — idempotent upsert key for rollup jobs

---

## Phase 2 — Domain Model (JPA Entities + Repositories)

**Goal:** map the tables to JPA entities and define repository interfaces. No business logic here.

### 2.1 `RequestLog` entity

Package: `com.dmdr.personal.portal.observability.model`

```java
@Entity
@Table(name = "request_log")
public class RequestLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String path;
    private String templatePath;
    private String method;
    private Integer status;
    private Long durationMs;
    private Long userId;
    private LocalDateTime createdAt;
    private String errorCode;
    @Column(columnDefinition = "TEXT") private String errorMessage;
    @Column(columnDefinition = "TEXT") private String stackTrace;
    // getters / setters / builder
}
```

### 2.2 `EndpointStat` entity

Package: `com.dmdr.personal.portal.observability.model`

```java
@Entity
@Table(name = "endpoint_stats")
public class EndpointStat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
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
    // getters / setters
}
```

### 2.3 `RequestLogRepository`

Package: `com.dmdr.personal.portal.observability.repository`

```java
public interface RequestLogRepository extends JpaRepository<RequestLog, Long>,
                                               JpaSpecificationExecutor<RequestLog> {

    /** Delete success rows older than the given cutoff. */
    @Modifying
    @Query("DELETE FROM RequestLog r WHERE r.status >= 200 AND r.status < 300 AND r.createdAt < :cutoff")
    int deleteSuccessRowsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** Delete failure rows (non-2xx) older than the given cutoff. */
    @Modifying
    @Query("DELETE FROM RequestLog r WHERE (r.status < 200 OR r.status >= 300) AND r.createdAt < :cutoff")
    int deleteFailureRowsOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Fetch rows within a time window for rollup processing.
     * The caller provides a closed UTC day boundary.
     */
    List<RequestLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
```

### 2.4 `EndpointStatRepository`

Package: `com.dmdr.personal.portal.observability.repository`

```java
public interface EndpointStatRepository extends JpaRepository<EndpointStat, Long> {

    Optional<EndpointStat> findByBucketStartAndMethodAndTemplatePath(
        LocalDate bucketStart, String method, String templatePath);

    List<EndpointStat> findByBucketStartBetweenAndTemplatePath(
        LocalDate from, LocalDate to, String templatePath);
}
```

---

## Phase 3 — Core Capture Support

**Goal:** define the enums, value objects, classification logic, and async writing infrastructure
that the filter (Phase 4) will depend on.

### 3.1 `RequestOutcome` enum

Package: `com.dmdr.personal.portal.observability.model`

```java
public enum RequestOutcome {
    SUCCESS,           // 2xx
    AUTH_ERROR,        // 401 or 403
    CLIENT_ERROR,      // other 4xx (excluding 401 and 403)
    SERVER_ERROR,      // 5xx
    OTHER_NON_SUCCESS  // 3xx and any other non-2xx not covered above
}
```

### 3.2 `RequestLogEvent` (capture-time DTO)

Package: `com.dmdr.personal.portal.observability.capture`

Immutable value object built by the filter after the response is committed. Passed to the async
writer.

```java
public final class RequestLogEvent {
    private final String path;
    private final String templatePath;
    private final String method;
    private final int status;
    private final long durationMs;
    private final Long userId;            // nullable
    private final LocalDateTime capturedAt;
    private final String errorCode;       // nullable
    private final String errorMessage;    // nullable
    private final String stackTrace;      // nullable; already truncated at creation

    // all-args constructor + getters (no setters — immutable)
}
```

### 3.3 `RequestOutcomeClassifier`

Package: `com.dmdr.personal.portal.observability.capture`

```java
public final class RequestOutcomeClassifier {

    /** Returns the outcome bucket for a given HTTP status code. */
    public static RequestOutcome classify(int status) { ... }

    /** Returns true if the status code is considered a success (2xx). */
    public static boolean isSuccess(int status) { ... }
}
```

### 3.4 `StackTraceTruncator`

Package: `com.dmdr.personal.portal.observability.capture`

```java
public final class StackTraceTruncator {

    public static final int MAX_BYTES = 65_536; // 64 KB
    public static final String TRUNCATION_MARKER = "\n... [truncated]";

    /**
     * Truncates the stack trace string to MAX_BYTES encoded as UTF-8,
     * appending TRUNCATION_MARKER when the string is cut.
     * Returns null input unchanged.
     */
    public static String truncate(String stackTrace) { ... }
}
```

### 3.5 `RequestLogMapper`

Package: `com.dmdr.personal.portal.observability.capture`

Converts a `RequestLogEvent` to a `RequestLog` JPA entity ready for persistence.

```java
public final class RequestLogMapper {

    /** Maps a capture event to a persistable RequestLog entity. */
    public static RequestLog toEntity(RequestLogEvent event) { ... }
}
```

### 3.6 `RequestLogWriter` (async service)

Package: `com.dmdr.personal.portal.observability.capture`

Spring `@Service`. Accepts `RequestLogEvent` objects from the filter and persists them
asynchronously so request threads are not blocked on DB I/O.

```java
@Service
public class RequestLogWriter {

    /**
     * Enqueues the event for async persistence.
     * Returns immediately. Best-effort: bounded row loss on crash is acceptable.
     */
    public void enqueue(RequestLogEvent event) { ... }

    /**
     * Internal method executed on the named async executor.
     * Maps event to entity via RequestLogMapper and calls repository.save().
     */
    @Async("observabilityExecutor")
    void persist(RequestLogEvent event) { ... }
}
```

**Thread-pool configuration:** define a named `ThreadPoolTaskExecutor` bean (`observabilityExecutor`)
in an `@Configuration` class (see Phase 8). Recommended defaults: core=2, max=4,
queue-capacity=1000, reject-policy=Discard (acceptable loss under backpressure).

---

## Phase 4 — Servlet Filter

**Goal:** intercept every request, measure wall-clock time, read `templatePath` from MVC request
attributes after the handler completes, apply prefix exclusion rules, and enqueue a
`RequestLogEvent` after the response is committed.

### 4.1 `ObservabilityRequestContext` (request-scoped holder)

Package: `com.dmdr.personal.portal.observability.filter`

A Spring `@Component @RequestScope` bean. The exception handler (Phase 5) writes error details
into it; the filter reads them after the chain returns.

```java
@Component
@RequestScope
public class ObservabilityRequestContext {

    private String errorCode;
    private String errorMessage;
    private String rawStackTrace; // not yet truncated; truncation happens in the filter

    /** Called by GlobalExceptionHandler on any mapped exception. */
    public void setErrorDetails(String errorCode, String errorMessage, String rawStackTrace) { ... }

    public String getErrorCode() { ... }
    public String getErrorMessage() { ... }
    public String getRawStackTrace() { ... }
}
```

### 4.2 `RequestObservabilityFilter`

Package: `com.dmdr.personal.portal.observability.filter`

Extends `OncePerRequestFilter`. Registered with an explicit `@Order` — placed after the Spring
Security filter chain so the `SecurityContext` is populated and `userId` can be resolved.

```java
@Component
@Order(/* Ordered.LOWEST_PRECEDENCE - 10, or a fixed value after security */)
public class RequestObservabilityFilter extends OncePerRequestFilter {

    /**
     * Core interception logic:
     * 1. Record start time.
     * 2. Call filterChain.doFilter() in a try/finally block.
     * 3. After chain: resolve templatePath, userId, status, duration.
     * 4. Apply shouldSkip() rules.
     * 5. Read error details from ObservabilityRequestContext.
     * 6. Build RequestLogEvent (with truncated stackTrace) and call writer.enqueue().
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException { ... }

    /**
     * Reads Spring MVC's HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE from the request.
     * Returns the sentinel string "UNKNOWN" if the attribute is absent (e.g. unmatched 404).
     */
    private String resolveTemplatePath(HttpServletRequest request) { ... }

    /**
     * Extracts the authenticated userId from the SecurityContext.
     * Returns null if the request is unauthenticated or the principal type is unrecognized.
     */
    private Long resolveUserId() { ... }

    /**
     * Returns true if this request must be silently skipped (no RequestLog row):
     * - Path starts with /actuator/ → always skip.
     * - Path starts with /admin/ AND status is 2xx → skip (errors-only rule).
     * - Path matches static asset patterns (configurable suffix list) → skip.
     */
    private boolean shouldSkip(String path, int status) { ... }
}
```

---

## Phase 5 — Exception Handler Integration

**Goal:** populate `ObservabilityRequestContext` with `errorCode`, `errorMessage`, and
`stackTrace` from the existing `GlobalExceptionHandler` so the filter can attach them to the
`RequestLogEvent`.

### 5.1 Modifications to `GlobalExceptionHandler`

Location: `personal-portal-core` → existing `GlobalExceptionHandler` (`@RestControllerAdvice`)

Inject `ObservabilityRequestContext` (constructor injection) and add a call to
`setErrorDetails(...)` inside every `@ExceptionHandler` method after building the error
response DTO, before returning it.

```java
// Injected via constructor:
private final ObservabilityRequestContext observabilityContext;

// Called at the end of each @ExceptionHandler method body:
observabilityContext.setErrorDetails(
    errorResponse.getCode(),
    errorResponse.getMessage(),
    stackTraceAsString(exception)
);

// Private utility added to GlobalExceptionHandler:
private static String stackTraceAsString(Throwable t) { ... }
```

**Coverage note:** requests that fail before reaching a controller (e.g. Spring Security 401/403,
filter-level errors) will not pass through `GlobalExceptionHandler`. For those requests,
`ObservabilityRequestContext` will have null error fields — this is acceptable. The filter still
persists the row with `status=401/403` and null `errorCode`/`errorMessage`/`stackTrace`.

---

## Phase 6 — Scheduled Jobs

**Goal:** implement the two background maintenance jobs: detail-row retention purge and daily
aggregate rollup.

### 6.1 `RequestLogRetentionJob`

Package: `com.dmdr.personal.portal.observability.job`

```java
@Component
public class RequestLogRetentionJob {

    /**
     * Executes the retention purge.
     * Default schedule: daily at 02:00 UTC (configurable via properties).
     */
    @Scheduled(cron = "${observability.request-log.retention-cron:0 0 2 * * *}",
               zone = "UTC")
    public void purgeExpiredRows() { ... }

    /** Deletes 2xx rows whose createdAt is before the success retention cutoff. */
    private int purgeSuccessRows(LocalDateTime cutoff) { ... }

    /** Deletes non-2xx rows whose createdAt is before the failure retention cutoff. */
    private int purgeFailureRows(LocalDateTime cutoff) { ... }
}
```

### 6.2 `EndpointStatsRollupJob`

Package: `com.dmdr.personal.portal.observability.job`

Aggregates closed UTC days from `request_log` into daily `endpoint_stats` rows. Must be
**idempotent**: processing a day that already has a bucket row must produce the same result as
the first run (use upsert / `INSERT ... ON CONFLICT DO UPDATE`).

```java
@Component
public class EndpointStatsRollupJob {

    /**
     * Rolls up all complete UTC days that have not yet been fully aggregated.
     * "Complete" means bucket_start < today(UTC).
     * Default schedule: top of every hour (configurable).
     */
    @Scheduled(cron = "${observability.request-log.rollup-cron:0 0 * * * *}",
               zone = "UTC")
    public void rollup() { ... }

    /**
     * Aggregates all RequestLog rows for the given UTC calendar day,
     * grouped by (method, templatePath), then upserts one EndpointStat per group.
     */
    private void rollupDay(LocalDate day) { ... }

    /**
     * Merges a list of RequestLog rows into an EndpointStat instance.
     * Uses existing stat row if present (for upsert path), otherwise creates a new one.
     */
    private EndpointStat buildStat(LocalDate day,
                                   String method,
                                   String templatePath,
                                   List<RequestLog> rows,
                                   Optional<EndpointStat> existing) { ... }
}
```

---

## Phase 7 — Read-Only Admin API

**Goal:** expose a secured, paginated REST surface for operators to inspect `RequestLog` rows.

### 7.1 Request/response DTOs

Package: `com.dmdr.personal.portal.observability.dto`

```java
/** Query parameters for the paginated list endpoint. */
public class RequestLogFilterRequest {
    private LocalDate dateFrom;       // inclusive; maps to createdAt >= dateFrom 00:00 UTC
    private LocalDate dateTo;         // inclusive; maps to createdAt < dateTo+1 00:00 UTC
    private Integer status;
    private String templatePath;
    private String method;
    private Long userId;
    private String errorCode;
    private int page = 0;             // 0-based
    private int size = 50;            // max 100
}

/** List-view projection (no stackTrace). */
public class RequestLogDto {
    private Long id;
    private String path;
    private String templatePath;
    private String method;
    private Integer status;
    private Long durationMs;
    private Long userId;
    private LocalDateTime createdAt;
    private String errorCode;
    private String errorMessage;
}

/** Detail-view projection (includes stackTrace). */
public class RequestLogDetailDto extends RequestLogDto {
    private String stackTrace;
}

/** Paginated list response. */
public class RequestLogPageResponse {
    private List<RequestLogDto> items;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
```

### 7.2 `RequestLogQueryService`

Package: `com.dmdr.personal.portal.observability.service`

```java
@Service
public class RequestLogQueryService {

    /**
     * Returns a paginated, filtered page of RequestLog rows.
     * Builds a JPA Specification from the filter for dynamic predicate composition.
     */
    public RequestLogPageResponse findAll(RequestLogFilterRequest filter) { ... }

    /**
     * Returns full detail for a single RequestLog row including stackTrace.
     * Throws an appropriate not-found exception if the id does not exist.
     */
    public RequestLogDetailDto findById(Long id) { ... }

    /** Assembles a JPA Specification from the non-null filter fields. */
    private Specification<RequestLog> toSpecification(RequestLogFilterRequest filter) { ... }

    /** Maps a RequestLog entity to RequestLogDto (no stackTrace). */
    private RequestLogDto toDto(RequestLog entity) { ... }

    /** Maps a RequestLog entity to RequestLogDetailDto (includes stackTrace). */
    private RequestLogDetailDto toDetailDto(RequestLog entity) { ... }
}
```

### 7.3 `RequestLogAdminController`

Package: `com.dmdr.personal.portal.controller.admin`

```java
@RestController
@RequestMapping("/api/v1/admin/observability/request-log")
@PreAuthorize("hasRole('ADMIN')")
public class RequestLogAdminController {

    /**
     * GET /api/v1/admin/observability/request-log
     * Accepts all RequestLogFilterRequest fields as query parameters.
     * Returns paginated RequestLogPageResponse.
     */
    @GetMapping
    public ResponseEntity<RequestLogPageResponse> list(
        @ModelAttribute RequestLogFilterRequest filter) { ... }

    /**
     * GET /api/v1/admin/observability/request-log/{id}
     * Returns full RequestLogDetailDto including stackTrace.
     * Returns 404 if not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RequestLogDetailDto> detail(@PathVariable Long id) { ... }
}
```

### 7.4 `EndpointStatsAdminController` (optional, same phase)

Package: `com.dmdr.personal.portal.controller.admin`

```java
@RestController
@RequestMapping("/api/v1/admin/observability/endpoint-stats")
@PreAuthorize("hasRole('ADMIN')")
public class EndpointStatsAdminController {

    /**
     * GET /api/v1/admin/observability/endpoint-stats
     * Query params: templatePath (required), dateFrom, dateTo, method (optional).
     * Returns list of EndpointStat rows for the requested range.
     */
    @GetMapping
    public ResponseEntity<List<EndpointStat>> query(
        @RequestParam String templatePath,
        @RequestParam LocalDate dateFrom,
        @RequestParam LocalDate dateTo,
        @RequestParam(required = false) String method) { ... }
}
```

---

## Phase 8 — Configuration and Testing

### 8.1 `ObservabilityProperties`

Package: `com.dmdr.personal.portal.observability.config`

```java
@ConfigurationProperties(prefix = "observability.request-log")
public class ObservabilityProperties {
    private int retentionSuccessDays = 7;
    private int retentionFailureDays = 30;
    private String retentionCron = "0 0 2 * * *";
    private String rollupCron = "0 0 * * * *";
    private int asyncQueueCapacity = 1000;
    private int asyncCorePoolSize = 2;
    private int asyncMaxPoolSize = 4;
    // getters / setters
}
```

### 8.2 `ObservabilityConfig`

Package: `com.dmdr.personal.portal.observability.config`

```java
@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
@EnableAsync
@EnableScheduling
public class ObservabilityConfig {

    /**
     * Named executor used by RequestLogWriter.
     * Reject policy: Discard (acceptable loss is by design).
     */
    @Bean("observabilityExecutor")
    public Executor observabilityExecutor(ObservabilityProperties props) { ... }
}
```

### 8.3 `application.properties` additions

```properties
# Detail row retention windows
observability.request-log.retention-success-days=7
observability.request-log.retention-failure-days=30
observability.request-log.retention-cron=0 0 2 * * *

# Aggregate rollup schedule
observability.request-log.rollup-cron=0 0 * * * *

# Async writer thread pool
observability.request-log.async-core-pool-size=2
observability.request-log.async-max-pool-size=4
observability.request-log.async-queue-capacity=1000
```

### 8.4 Unit tests

| Class under test | Scenarios to cover |
|--|--|
| `RequestOutcomeClassifier` | 200 → SUCCESS; 204 → SUCCESS; 301 → OTHER_NON_SUCCESS; 400 → CLIENT_ERROR; 401 → AUTH_ERROR; 403 → AUTH_ERROR; 404 → CLIENT_ERROR; 500 → SERVER_ERROR; 503 → SERVER_ERROR |
| `StackTraceTruncator` | null input → null; short string → unchanged; exactly MAX_BYTES → unchanged; MAX_BYTES+1 → truncated with marker |
| `RequestLogMapper` | all fields from `RequestLogEvent` map to correct `RequestLog` fields |
| `RequestObservabilityFilter.shouldSkip` | `/actuator/health` 200 → skip; `/admin/sba/x` 200 → skip; `/admin/sba/x` 500 → do not skip; `/api/v1/admin/x` 200 → do not skip; `/api/v1/admin/x` 500 → do not skip |
| `RequestOutcomeClassifier` (aggregate mapping) | each `RequestOutcome` value maps to the expected `EndpointStat` counter field |

### 8.5 Integration tests

| Scenario | Expected result |
|--|--|
| `POST /api/v1/auth/login` succeeds (200) | 1 `RequestLog` row; `status=200`; `errorCode=null` |
| `POST /api/v1/auth/login` bad credentials (401) | 1 `RequestLog` row; `status=401` |
| `GET /actuator/health` | 0 `RequestLog` rows |
| `GET /admin/sba/applications` → 200 | 0 `RequestLog` rows |
| `GET /admin/sba/applications` → 500 | 1 `RequestLog` row; `status=500` |
| `GET /api/v1/nonexistent` → 404 | 1 `RequestLog` row; `templatePath="UNKNOWN"` |
| Mapped exception in controller → 422 | 1 `RequestLog` row with non-null `errorCode`, `errorMessage`, `stackTrace` |
| `RequestLogRetentionJob` | Success rows > 7d old deleted; failure rows > 30d old deleted; recent rows untouched; aggregate rows untouched |
| `EndpointStatsRollupJob` | `EndpointStat` row created with correct counts; second run on same day is idempotent (same counts) |
| Admin API `GET /api/v1/admin/observability/request-log` without ADMIN role | 403 Forbidden |
| Admin API with filter `status=500` | returns only 5xx rows |

---

## Implementation Sequence Summary

```
Phase 1  Flyway: create request_log and endpoint_stats tables + indexes
Phase 2  JPA entities (RequestLog, EndpointStat) + repositories
Phase 3  RequestOutcome enum, RequestLogEvent DTO,
         RequestOutcomeClassifier, StackTraceTruncator,
         RequestLogMapper, RequestLogWriter + observabilityExecutor bean
Phase 4  ObservabilityRequestContext (@RequestScope),
         RequestObservabilityFilter
Phase 5  GlobalExceptionHandler wired to ObservabilityRequestContext
Phase 6  RequestLogRetentionJob, EndpointStatsRollupJob
Phase 7  Admin API DTOs, RequestLogQueryService,
         RequestLogAdminController, EndpointStatsAdminController
Phase 8  ObservabilityProperties, ObservabilityConfig,
         application.properties keys, unit tests, integration tests
```

Each phase introduces dependencies on all previous phases. Specifically:
- Phase 3 requires Phase 2 entities to compile `RequestLogMapper` and `RequestLogWriter`.
- Phase 4 requires Phase 3 `RequestLogEvent` and `RequestLogWriter`.
- Phase 5 requires Phase 4 `ObservabilityRequestContext` to be in the Spring context.
- Phase 6 requires Phase 2 repositories and Phase 3 classifier logic.
- Phase 7 requires Phase 2 repositories and entities.
- Phase 8 configuration wires all beans introduced in Phases 3–7 together.
