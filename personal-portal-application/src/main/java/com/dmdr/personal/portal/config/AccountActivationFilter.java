package com.dmdr.personal.portal.config;

import com.dmdr.personal.portal.service.exception.PortalErrorCode;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.repository.UserRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AccountActivationFilter extends OncePerRequestFilter {

    private static final String ACCOUNT_NOT_ACTIVATED_MESSAGE = "Account must be activated.";
    private static final Pattern PUBLIC_API_PATH = Pattern.compile("^/api/v\\d+/public(?:/.*)?$");
    private static final Pattern ADMIN_API_PATH = Pattern.compile("^/api/v\\d+/admin(?:/.*)?$");
    private static final Pattern AUTH_API_PATH = Pattern.compile("^/api/v\\d+/auth(?:/.*)?$");
    private static final Pattern ACCOUNT_ACTIVATION_API_PATH = Pattern.compile("^/api/v\\d+/user/account/activation(?:/.*)?$");
    private static final Pattern USER_PROFILE_API_PATH = Pattern.compile("^/api/v\\d+/user/profile(?:/.*)?$");

    private final UserRepository userRepository;

    public AccountActivationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_API_PATH.matcher(path).matches()
                || ADMIN_API_PATH.matcher(path).matches()
                || AUTH_API_PATH.matcher(path).matches()
                || ACCOUNT_ACTIVATION_API_PATH.matcher(path).matches()
                || USER_PROFILE_API_PATH.matcher(path).matches();
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

        if (!(authentication.getPrincipal() instanceof UUID userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<User> user;
        try {
            user = userRepository.findUserById(userId);
        } catch (Exception e) {
            log.error("Failed to validate account activation for user {}: {}", userId, e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }

        if (user.isPresent() && !user.get().isActive()) {
            response.setStatus(PortalErrorCode.ACCOUNT_NOT_VERIFIED.getHttpCode());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"" + PortalErrorCode.ACCOUNT_NOT_VERIFIED.getCode()
                    + "\",\"message\":\"" + ACCOUNT_NOT_ACTIVATED_MESSAGE + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
