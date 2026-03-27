package com.dmdr.personal.portal.admin.observability;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for REST request log observability (retention jobs, rollup, async persistence).
 */
@ConfigurationProperties(prefix = "observability.request-log")
@Getter
@Setter
public class RequestLogObservabilityProperties {

	/**
	 * Days to retain successful request log rows before cleanup.
	 */
	private int retentionSuccessDays = 7;

	/**
	 * Days to retain failed request log rows before cleanup.
	 */
	private int retentionFailureDays = 30;

	/**
	 * Cron for retention cleanup (Spring {@code @Scheduled} — use {@code zone} UTC on the job).
	 */
	private String retentionCron = "0 0 2 * * *";

	/**
	 * Enables/disables the retention scheduled job.
	 */
	private boolean retentionJobEnabled = true;

	/**
	 * Max rows to delete per SQL statement during retention cleanup.
	 */
	private int retentionDeleteBatchSize = 500;

	/**
	 * Upper bound for delete batches in one retention run to avoid long DB spikes.
	 */
	private int retentionMaxBatchesPerRun = 200;

	/**
	 * Cron for hourly rollup (Spring {@code @Scheduled} — use {@code zone} UTC on the job).
	 */
	private String rollupCron = "0 0 * * * *";

	/**
	 * Enables/disables the rollup scheduled job.
	 */
	private boolean rollupJobEnabled = true;

	/**
	 * Max rows to process per rollup execution transaction.
	 */
	private int rollupBatchSize = 1000;

	private int asyncCorePoolSize = 2;

	private int asyncMaxPoolSize = 4;

	private int asyncQueueCapacity = 1000;

	/**
	 * Max in-memory request-log records per flush batch.
	 */
	private int asyncFlushBatchSize = 100;

	/**
	 * Max interval in milliseconds before buffered records are flushed.
	 */
	private long asyncFlushIntervalMs = 1000;

	/**
	 * Enables/disables request body capture for observability logs.
	 */
	private boolean requestBodyEnabled = true;

	/**
	 * Maximum number of characters persisted for sanitized request body payload.
	 */
	private int requestBodyMaxChars = 16_384;

	/**
	 * Placeholder value for sensitive fields in captured request bodies.
	 */
	private String requestBodyRedactionPlaceholder = "***";

	/**
	 * Placeholder value when request body is non-JSON and omitted from persistence.
	 */
	private String requestBodyNonJsonPlaceholder = "[non-json omitted]";

	/**
	 * Placeholder value when request body capture/sanitization fails.
	 */
	private String requestBodySanitizationFailedPlaceholder = "[sanitization_failed]";

	/**
	 * Lower-cased field names considered sensitive for recursive JSON body redaction.
	 */
	private List<String> requestBodySensitiveKeys = List.of(
		"password",
		"newpassword",
		"oldpassword",
		"token",
		"accesstoken",
		"refreshtoken",
		"email",
		"phone",
		"firstname",
		"lastname",
		"fullname",
		"address",
		"birthdate"
	);

	/**
	 * Enables/disables request header metadata capture.
	 */
	private boolean requestHeaderMetadataEnabled = true;

	/**
	 * Enables/disables response header metadata capture.
	 */
	private boolean responseHeaderMetadataEnabled = true;

	/**
	 * Maximum number of characters persisted for sanitized header metadata payload.
	 */
	private int headerMetadataMaxChars = 16_384;

	/**
	 * Placeholder value for sensitive headers in captured metadata.
	 */
	private String headerRedactionPlaceholder = "***";

	/**
	 * Placeholder value when header metadata sanitization fails.
	 */
	private String headerSanitizationFailedPlaceholder = "[header_sanitization_failed]";

	/**
	 * Case-insensitive blacklist of header names whose values must be redacted.
	 */
	private List<String> headerSensitiveKeys = List.of(
		"authorization",
		"proxy-authorization",
		"cookie",
		"set-cookie",
		"x-api-key",
		"x-auth-token",
		"x-csrf-token",
		"x-forwarded-for",
		"forwarded",
		"x-real-ip"
	);

	/**
	 * Enables/disables sanitization of error messages and stack traces.
	 * These fields are unstructured and can contain secrets/PII even when headers/bodies are sanitized.
	 */
	private boolean errorTextEnabled = true;

	/**
	 * Maximum number of characters persisted for sanitized error message / stack trace text.
	 */
	private int errorTextMaxChars = 16_384;

	/**
	 * Placeholder value for sensitive fragments detected in error text.
	 */
	private String errorTextRedactionPlaceholder = "***";

	/**
	 * Placeholder value when error text sanitization fails.
	 */
	private String errorTextSanitizationFailedPlaceholder = "[error_sanitization_failed]";

	/**
	 * Case-insensitive list of key names whose values should be redacted in error text (e.g. "token=...").
	 * Keys are normalized by removing non-alphanumeric characters.
	 */
	private List<String> errorTextSensitiveKeys = List.of(
		"password",
		"newpassword",
		"oldpassword",
		"token",
		"accesstoken",
		"refreshtoken",
		"secret",
		"apikey",
		"xapikey",
		"clientsecret",
		"session",
		"cookie",
		"authorization",
		"otp",
		"code",
		"verificationcode"
	);

}
