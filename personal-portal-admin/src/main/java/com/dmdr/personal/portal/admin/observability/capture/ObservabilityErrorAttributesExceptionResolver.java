package com.dmdr.personal.portal.admin.observability.capture;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

/**
 * Observability fallback that records exception attributes for request logs.
 * See docs/observability/dev/rest-request-observability-impl-plan-composer.md (Step C2).
 */
@Component
public class ObservabilityErrorAttributesExceptionResolver implements HandlerExceptionResolver, Ordered {

    private final RequestLogErrorContext requestLogErrorContext;

    public ObservabilityErrorAttributesExceptionResolver(RequestLogErrorContext requestLogErrorContext) {
        this.requestLogErrorContext = Objects.requireNonNull(requestLogErrorContext, "requestLogErrorContext must not be null");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    @Nullable
    public ModelAndView resolveException(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @Nullable Object handler,
        @NonNull Exception ex
    ) {
        requestLogErrorContext.recordApiError(request, null, ex.getMessage(), ex);
        return null;
    }
}
