# Deploy guide (env configuration)

This document lists **environment variables** that must (or may) be set before deploying `portal-application`, based on:

## Required env vars (production)

These have **no safe production defaults** and are referenced as `${VAR}`.

- **`SPRING_DATASOURCE_URL`**: JDBC URL for PostgreSQL (e.g. `jdbc:postgresql://host:5432/db`).
- **`DB_USERNAME`**: Database username.
- **`DB_PASSWORD`**: Database password.

- **`JWT_SECRET_KEY`**: Secret used to sign JWTs (keep private; rotate carefully).
- **`JWT_ISSUER`**: JWT issuer (dev default exists in `application-dev.properties`).
- **`JWT_AUDIENCE`**: JWT audience (dev default exists in `application-dev.properties`).

- **`APP_FRONTEND_URL`**: Public frontend/base URL used to build links in emails (e.g. `https://portal.example.com`).

- **`CORS_ALLOWED_ORIGINS`**: Allowed origins for CORS in production (comma-separated).

- **`SPRING_MAIL_HOST`**: SMTP host.
- **`SPRING_MAIL_PORT`**: SMTP port (typically `587` for STARTTLS or `465` for SMTPS).
- **`SPRING_MAIL_FROM_EMAIL`**: “From” email address used by the application.
- **`SPRING_MAIL_FROM_NAME`**: “From” display name used by the application (dev default exists in `application-dev.properties`).

- **`ADMIN_USER_EMAIL`**: Admin bootstrap/login email (ensure you manage this securely in prod).
- **`ADMIN_USER_PASSWORD`**: Admin bootstrap/login password (ensure you manage this securely in prod).

- **`SBA_CLIENT_USERNAME`**: Spring Boot Admin (embedded) basic-auth username.
- **`SBA_CLIENT_PASSWORD`**: Spring Boot Admin (embedded) basic-auth password.

## Optional env vars (production)

These are referenced as `${VAR:default}` or `${VAR:}` (empty default), meaning the app can start without them (but features may be disabled or behavior changes).

## Not env vars (Spring properties)

The production config also references Spring properties like `${server.port:8080}`. These are **not** OS environment variables; configure them via Spring config mechanisms (env mapping, JVM system properties, or additional `.properties`/YAML).

### JWT settings (optional overrides)

- **`JWT_ACCESS_TOKEN_TTL_MINUTES`**: Defaults to `10`.
- **`JWT_REFRESH_TOKEN_IDLE_TTL_MINUTES`**: Defaults to `1440`.
- **`JWT_REFRESH_TOKEN_ABSOLUTE_TTL_MINUTES`**: Defaults to `10080`.
- **`JWT_REFRESH_TOKEN_CLEANUP_INTERVAL_MS`**: Defaults to `3600000`.

### Account verification (optional overrides)

- **`ACCOUNT_VERIFICATION_CODE_EXPIRY_MINUTES`**: Defaults to `60`.
- **`ACCOUNT_VERIFICATION_CODE_MAX_ATTEMPTS`**: Defaults to `5`.
- **`ACCOUNT_VERIFICATION_RESEND_MAX_PER_DAY`**: Defaults to `5`.

### S3 / object storage (optional)

If you use object storage, set these (otherwise values default to empty / disabled depending on usage).

- **`CLOUD_AWS_REGION_STATIC`**: Defaults to empty.
- **`CLOUD_AWS_CREDENTIALS_ACCESS_KEY`**: Defaults to empty.
- **`CLOUD_AWS_CREDENTIALS_SECRET_KEY`**: Defaults to empty.
- **`CLOUD_AWS_S3_ENDPOINT`**: Defaults to empty (set for S3-compatible providers like MinIO).
- **`CLOUD_AWS_S3_PATH_STYLE_ACCESS`**: Defaults to `false`.
- **`AWS_S3_BUCKET_NAME`**: Defaults to empty.

### SMTP auth / TLS (optional overrides)

- **`SPRING_MAIL_USERNAME`**: Defaults to empty.
- **`SPRING_MAIL_PASSWORD`**: Defaults to empty.
- **`SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH`**: Defaults to `true`.
- **`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE`**: Defaults to `true`.
- **`SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED`**: Defaults to `true`.

### Email templates directory (optional)

- **`SPRING_MAIL_TEMPLATES_DIRECTORY`**: Defaults to empty. When set, templates are loaded from this directory first.

