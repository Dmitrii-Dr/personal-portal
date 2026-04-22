package com.dmdr.personal.portal.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@AfterEach
	void tearDown() {
		MDC.clear();
	}

	@Test
	void mdcContainsCorrelationIdDuringChain_andClearedAfter() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> seenDuringChain = new AtomicReference<>();
		FilterChain chain = (req, res) -> seenDuringChain.set(
				Objects.requireNonNull(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)));

		filter.doFilter(request, response, chain);

		assertThat(seenDuringChain.get())
				.isNotNull()
				.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
		assertThat(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)).isNull();
		assertThat(response.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isEqualTo(seenDuringChain.get());
	}

	@Test
	void usesXCorrelationIdHeaderWhenValidUuid() throws ServletException, IOException {
		String expectedId = "550e8400-e29b-41d4-a716-446655440000";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationIdFilter.HEADER_CORRELATION_ID, expectedId);
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> seenDuringChain = new AtomicReference<>();
		FilterChain chain = (req, res) -> seenDuringChain.set(
				Objects.requireNonNull(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)));

		filter.doFilter(request, response, chain);

		assertThat(seenDuringChain.get()).isEqualTo(expectedId);
		assertThat(response.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isEqualTo(expectedId);
	}

	@Test
	void invalidHeaderGeneratesNewUuid() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(CorrelationIdFilter.HEADER_CORRELATION_ID, "not-a-uuid");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicReference<String> seenDuringChain = new AtomicReference<>();
		FilterChain chain = (req, res) -> seenDuringChain.set(
				Objects.requireNonNull(MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID)));

		filter.doFilter(request, response, chain);

		assertThat(seenDuringChain.get())
				.isNotNull()
				.isNotEqualTo("not-a-uuid")
				.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
		assertThat(response.getHeader(CorrelationIdFilter.HEADER_CORRELATION_ID)).isEqualTo(seenDuringChain.get());
	}
}
