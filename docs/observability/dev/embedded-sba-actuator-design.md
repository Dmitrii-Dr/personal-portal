# Design: Embedded Spring Boot Admin (SBA) + Actuator

## Overview

Integrate an **embedded** Spring Boot Admin dashboard and Actuator into the existing Personal Portal application. The SBA Server and SBA Client run inside the **same** application instance. The app registers itself with its own admin server and serves the admin UI on a dedicated path.

---

## 1. Dependencies

**Location:** Parent BOM / `personal-portal-admin` module (and consumed by `personal-portal-application`).

Use **latest compatible** versions for:

- `spring-boot-starter-actuator`
- `spring-boot-admin-starter-server`
- `spring-boot-admin-starter-client`

Versions should align with the project’s Spring Boot BOM (e.g. SBA 3.4.x for Spring Boot 3.x). Manage SBA versions in the parent `dependencyManagement` if not already.

---

## 2. Main Application Class

**Location:** Main Spring Boot application class (e.g. `PersonalPortalApplication`).

- Add **`@EnableAdminServer`** to enable the embedded SBA server.
- No separate main class or runnable module for admin; the single application is both server and client.

---

## 3. Application Configuration

**Location:** `application.yml` (or `.properties`) in the `personal-portal-application`.

| Concern | Configuration |
|--------|----------------|
| **Actuator** | Expose only endpoints needed for SBA: `management.endpoints.web.exposure.include=health,metrics,loggers,logfile` (optionally add `info`). Also harden public health: `management.endpoint.health.show-details=never` (optionally `management.endpoint.health.show-components=never`). |

| **SBA client self-registration** | Point the client at the same instance: `spring.boot.admin.client.url=http://localhost:${server.port}` so the app registers to itself. |

| **SBA UI path** | Serve the admin UI at `/admin/sba`: `spring.boot.admin.ui.base-url=/admin/sba`. |

| **Logging** | Set a fixed log file so SBA can show logs: `logging.file.name=logs/app.log` (or `logging.file.path` as appropriate). |

logging:
  file:
    name: logs/app.log
  logback:
    rollingpolicy:
      max-file-size: 10MB      # Размер одного файла лога до архивации (10-50MB оптимально)
      max-history: 14          # Сколько дней хранить старые архивы (например, 14 дней)
      total-size-cap: 500MB    # Максимальный размер ВСЕЙ папки с логами (очень важно для слабых серверов!)
      clean-history-on-start: false # Не удалять логи при перезапуске приложения

---

## 4. Security Configuration

**Context:** Existing Spring Security setup using JWT. WebSecurityConfig Class

**Goals:**

- Restrict admin and actuator access to **ADMIN** users only.
- Allow the embedded SBA **client** to call the server (register, heartbeat) without being blocked by CSRF.

### 4.1 Paths to Secure (ADMIN only)

- **`/admin/sba/**`** — SBA dashboard UI.
- **`/actuator/**`** — Actuator endpoints (used by SBA server to read app health/metrics).
- **`/instances/**`** — SBA server API (registration, instance management).

Apply role-based access (e.g. `hasRole("ADMIN")`) for these paths. Use the existing JWT-based authentication and role model. (see enum SystemRole)

### 4.2 CSRF Exclusions (Critical)

**Disable CSRF** for:

- **`/instances/**`**
- **`/actuator/**`**

Reason: The embedded SBA client sends **POST** requests to the same app (e.g. to register and push data). If CSRF is enabled for these paths, those requests get **403 Forbidden** and self-registration fails. Excluding these paths from CSRF (e.g. via `requestMatchers(...).csrf().disabled()`) fixes this while keeping CSRF for the rest of the app (e.g. web UI).

Keep CSRF **enabled** for other endpoints as per current security policy.

---

## 5. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  Personal Portal Application                 │
│  ┌─────────────────┐    ┌─────────────────┐                  │
│  │  SBA Server     │◄───│  SBA Client     │  (self-register) │
│  │  UI:/admin/sba/ │    │  POST /instances│                  │
│  └────────┬────────┘    └────────┬────────┘                  │
│           │                      │                            │
│           │   reads              │   exposes                  │
│           ▼                      ▼                            │
│  ┌─────────────────────────────────────┐                     │
│  │  Actuator (management.endpoints)    │                     │
│  │  exposure: health,metrics,loggers,  │                     │
│  │            logfile                  │                     │
│  └─────────────────────────────────────┘                     │
│                                                               │
│  Security: /admin/sba/, /actuator, /instances → ADMIN only      │
│            CSRF disabled for /instances, /actuator            │
└─────────────────────────────────────────────────────────────┘
```

---

## 6. Implementation Checklist (for later)

- [ ] Ensure dependencies (actuator, SBA server, SBA client) and versions in build.
- [ ] Add `@EnableAdminServer` on main application class.
- [ ] Add application properties: actuator exposure, SBA client URL, SBA UI base URL, logging file.
- [ ] Update `SecurityFilterChain`: protect `/admin/sba/**`, `/actuator/**`, `/instances/**` with ADMIN role.
- [ ] Disable CSRF for `/instances/**` and `/actuator/**` in the same security configuration.
- [ ] Verify self-registration (no 403 on POST) and dashboard at `/admin/sba`.
- [ ] Verify log file is created and visible in SBA (if applicable).

---

## 7. References

- [Spring Boot Admin – Server](https://docs.spring-boot-admin.com/current/server.html)
- [Spring Boot Admin – Client](https://docs.spring-boot-admin.com/current/client.html)
- [Spring Boot Actuator – Endpoints](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
