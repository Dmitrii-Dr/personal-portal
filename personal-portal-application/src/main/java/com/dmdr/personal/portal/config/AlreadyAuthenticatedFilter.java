package com.dmdr.personal.portal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class AlreadyAuthenticatedFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REGISTRY_PATH = "/api/v1/auth/registry";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if user is authenticated (not anonymous) and trying to access login or registry endpoints
        // JWT filter sets principal as email (String), anonymous users have "anonymousUser" as principal
        boolean isAuthenticated = authentication != null 
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof String
                && !"anonymousUser".equals(authentication.getPrincipal());

        if (isAuthenticated && (requestPath.equals(LOGIN_PATH) || requestPath.equals(REGISTRY_PATH))) {
            log.debug("Authenticated user attempted to access {} endpoint", requestPath);
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Already authenticated. Cannot access login/registry endpoints.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}

