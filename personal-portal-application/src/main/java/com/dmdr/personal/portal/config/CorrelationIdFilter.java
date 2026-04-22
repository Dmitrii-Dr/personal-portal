package com.dmdr.personal.portal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sets a per-request correlation UUID in {@link MDC} under {@link #MDC_CORRELATION_ID} for log patterns.
 * Accepts optional {@value #HEADER_CORRELATION_ID} or {@value #HEADER_REQUEST_ID} when valid UUID; otherwise generates one.
 * Echoes the id on the response as {@value #HEADER_CORRELATION_ID}.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String MDC_CORRELATION_ID = "correlationId";
	public static final String HEADER_CORRELATION_ID = "X-Correlation-ID";
	public static final String HEADER_REQUEST_ID = "X-Request-ID";

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		String correlationId = resolveCorrelationId(request);
		MDC.put(MDC_CORRELATION_ID, correlationId);
		response.setHeader(HEADER_CORRELATION_ID, correlationId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_CORRELATION_ID);
		}
	}

	private static String resolveCorrelationId(HttpServletRequest request) {
		String fromHeader = firstNonBlank(
				request.getHeader(HEADER_CORRELATION_ID),
				request.getHeader(HEADER_REQUEST_ID)
		);
		if (fromHeader != null) {
			try {
				return UUID.fromString(fromHeader.trim()).toString();
			} catch (IllegalArgumentException ignored) {
				// fall through to new UUID
			}
		}
		return UUID.randomUUID().toString();
	}

	private static String firstNonBlank(String a, String b) {
		if (StringUtils.hasText(a)) {
			return a;
		}
		if (StringUtils.hasText(b)) {
			return b;
		}
		return null;
	}
}
