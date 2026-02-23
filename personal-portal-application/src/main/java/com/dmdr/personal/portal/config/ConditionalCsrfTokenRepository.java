package com.dmdr.personal.portal.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;

public class ConditionalCsrfTokenRepository implements CsrfTokenRepository {

    public static final String ALLOW_CSRF_SAVE_ATTR = ConditionalCsrfTokenRepository.class.getName() + ".ALLOW_SAVE";

    private final CsrfTokenRepository delegate;

    public ConditionalCsrfTokenRepository(CsrfTokenRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return delegate.generateToken(request);
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        Object allow = request.getAttribute(ALLOW_CSRF_SAVE_ATTR);
        if (Boolean.TRUE.equals(allow)) {
            delegate.saveToken(token, request, response);
        }
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        return delegate.loadToken(request);
    }
}
