package com.dmdr.personal.portal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatchers;

@Configuration
public class CsrfConfig {

    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    // Make CSRF cookie persistent so mobile browsers don't drop it on app exit.
    // Keep it aligned with refresh token absolute TTL (in minutes).
    private final int csrfCookieMaxAgeSeconds;

    public CsrfConfig(@Value("${jwt.refresh-token-absolute-ttl-minutes:10080}") long refreshTokenAbsoluteTtlMinutes) {
        this.csrfCookieMaxAgeSeconds = Math.toIntExact(refreshTokenAbsoluteTtlMinutes * 60L);
    }

    @Bean
    @SuppressWarnings("deprecation")
    public CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName(CSRF_COOKIE_NAME);
        repository.setHeaderName(CSRF_HEADER_NAME);
        repository.setCookiePath("/");
        repository.setCookieMaxAge(csrfCookieMaxAgeSeconds);
        return new ConditionalCsrfTokenRepository(repository);
    }

    @Bean
    public CsrfTokenRequestHandler csrfTokenRequestHandler() {
        return new CsrfTokenRequestAttributeHandler();
    }

    @Bean
    public RequestMatcher csrfRequestMatcher() {
        PathPatternRequestMatcher.Builder matcherBuilder = PathPatternRequestMatcher.withDefaults();
        RequestMatcher refreshEndpoint = matcherBuilder.matcher(HttpMethod.POST, "/api/v1/auth/refresh");
        RequestMatcher logoutEndpoint = matcherBuilder.matcher(HttpMethod.POST, "/api/v1/auth/logout");
        RequestMatcher csrfRequiredPaths = RequestMatchers.anyOf(refreshEndpoint, logoutEndpoint);
        // Explicitly exclude SBA/actuator so embedded client can register (POST without CSRF token)
        RequestMatcher sbaPaths = RequestMatchers.anyOf(
                matcherBuilder.matcher("/admin/sba/instances/**"),
                matcherBuilder.matcher("/actuator/**"));
        return RequestMatchers.allOf(csrfRequiredPaths, RequestMatchers.not(sbaPaths));
    }
}
