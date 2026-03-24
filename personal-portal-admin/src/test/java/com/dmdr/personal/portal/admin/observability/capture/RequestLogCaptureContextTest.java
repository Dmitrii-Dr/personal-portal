package com.dmdr.personal.portal.admin.observability.capture;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RequestLogCaptureContextTest {

    @Test
    void attachAndCurrent_shouldStoreContextOnRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        Instant startedAt = Instant.parse("2026-03-23T10:15:30Z");

        RequestLogCaptureContext.attach(request, startedAt);

        assertThat(RequestLogCaptureContext.current(request)).isPresent();
        assertThat(RequestLogCaptureContext.current(request).orElseThrow().getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void current_shouldBeEmptyWhenNotAttached() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertThat(RequestLogCaptureContext.current(request)).isEmpty();
    }
}
