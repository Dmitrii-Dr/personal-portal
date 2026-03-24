package com.dmdr.personal.portal.admin.observability.capture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ObservabilityErrorAttributesExceptionResolverTest {

    @Test
    void resolveException_shouldRecordStackTraceAndMessageAndContinueResolverChain() {
        RequestAttributeRequestLogErrorContext context = new RequestAttributeRequestLogErrorContext();
        ObservabilityErrorAttributesExceptionResolver resolver = new ObservabilityErrorAttributesExceptionResolver(context);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RuntimeException error = new RuntimeException("from param parse");
        Object result = resolver.resolveException(request, response, new Object(), error);

        assertThat(result).isNull();
        assertThat(request.getAttribute(RequestLogAttributes.ERROR_CODE)).isNull();
        assertThat(request.getAttribute(RequestLogAttributes.ERROR_MESSAGE)).isEqualTo("from param parse");
        Object stackTrace = request.getAttribute(RequestLogAttributes.STACK_TRACE);
        assertThat(stackTrace).isInstanceOf(String.class);
        assertThat((String) stackTrace).contains("RuntimeException");
    }
}
