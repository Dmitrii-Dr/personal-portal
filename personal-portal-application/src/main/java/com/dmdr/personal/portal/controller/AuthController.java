package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.service.JwtService;
import com.dmdr.personal.portal.users.dto.AuthResponse;
import com.dmdr.personal.portal.users.dto.CreateUserRequest;
import com.dmdr.personal.portal.users.dto.ForgotPasswordRequest;
import com.dmdr.personal.portal.users.dto.LoginRequest;
import com.dmdr.personal.portal.users.dto.ResetPasswordRequest;
import com.dmdr.personal.portal.service.exception.PersonalPortalRuntimeException;
import com.dmdr.personal.portal.service.exception.PortalErrorCode;
import com.dmdr.personal.portal.users.model.Role;
import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.service.AccountVerificationService;
import com.dmdr.personal.portal.users.service.PasswordResetService;
import com.dmdr.personal.portal.users.service.RefreshTokenService;
import com.dmdr.personal.portal.users.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.dmdr.personal.portal.config.ConditionalCsrfTokenRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/api/v1/auth/refresh";

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;
    private final AccountVerificationService accountVerificationService;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(UserService userService,
            JwtService jwtService,
            PasswordResetService passwordResetService,
            RefreshTokenService refreshTokenService,
            AccountVerificationService accountVerificationService,
            CsrfTokenRepository csrfTokenRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
        this.refreshTokenService = refreshTokenService;
        this.accountVerificationService = accountVerificationService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        User user = userService.findByEmail(request.getEmail())
                .orElseThrow(() -> new PersonalPortalRuntimeException(PortalErrorCode.INVALID_EMAIL_PASSWORD));

        if (!userService.validatePassword(request.getPassword(), user.getPassword())) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.INVALID_EMAIL_PASSWORD);
        }

        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        RefreshTokenService.RefreshTokenIssueResult refreshResult = refreshTokenService.issueRefreshToken(user);
        String token = jwtService.generateToken(user.getId(), roles, refreshResult.sessionId());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setEmail(user.getEmail());
        response.setRoles(roles);

        ensureCsrfToken(httpRequest, httpResponse);

        log.info("User logged in successfully: {}", user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshResult.refreshToken(), refreshResult.expiresAtAbsolute()).toString())
                .body(response);
    }

    @PostMapping("/registry")
    public ResponseEntity<Void> registry(@Valid @RequestBody CreateUserRequest request) {
        // Validate that user doesn't exist
        if (userService.findByEmail(request.getEmail()).isPresent()) {
            throw new PersonalPortalRuntimeException(PortalErrorCode.EMAIL_ALREADY_IN_USE);
        }

        User user = userService.createUser(request);
        accountVerificationService.issueVerificationCode(user);

        log.info("User registered successfully: {}", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            RefreshTokenService.RefreshTokenRotationResult rotation = refreshTokenService.rotateRefreshToken(refreshToken);
            UUID userId = rotation.userId();
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Set<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());

            String token = jwtService.generateToken(user.getId(), roles, rotation.sessionId());

            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setEmail(user.getEmail());
            response.setRoles(roles);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(rotation.refreshToken(), rotation.expiresAtAbsolute()).toString())
                    .body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok("If an account with that email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok("Password successfully reset.");
    }

    private ResponseCookie buildRefreshCookie(String refreshToken, OffsetDateTime expiresAtAbsolute) {
        Duration maxAge = Duration.between(OffsetDateTime.now(), expiresAtAbsolute);
        long maxAgeSeconds = Math.max(0L, maxAge.getSeconds());

        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAgeSeconds)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private void ensureCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken token = csrfTokenRepository.loadToken(request);
        if (token == null) {
            request.setAttribute(ConditionalCsrfTokenRepository.ALLOW_CSRF_SAVE_ATTR, Boolean.TRUE);
            token = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(token, request, response);
        }
    }
}
