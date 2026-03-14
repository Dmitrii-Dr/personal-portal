package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.core.security.SystemRole;
import com.dmdr.personal.portal.core.state.HomePageActiveHolder;
import com.dmdr.personal.portal.service.exception.PortalErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
public class HomePageActiveFilter extends OncePerRequestFilter {

    private static final Pattern USER_API_PATH = Pattern.compile("^/api/v\\d+/user(?:/.*)?$");

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return !USER_API_PATH.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!HomePageActiveHolder.isActive() && !isAdmin(authentication)) {
            response.setStatus(PortalErrorCode.PORTAL_INACTIVE.getHttpCode());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"" + PortalErrorCode.PORTAL_INACTIVE.getCode()
                    + "\",\"message\":\"" + PortalErrorCode.PORTAL_INACTIVE.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> SystemRole.ADMIN.getAuthority().equals(authority.getAuthority()));
    }
}
