package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.users.model.User;
import com.dmdr.personal.portal.users.model.UserSession;
import com.dmdr.personal.portal.users.repository.UserSessionRepository;
import com.dmdr.personal.portal.users.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 32; // 256 bits
    private final long absoluteTtlMinutes;
    private final long idleTtlMinutes;

    private final UserSessionRepository userSessionRepository;

    public RefreshTokenServiceImpl(
            UserSessionRepository userSessionRepository,
            @Value("${jwt.refresh-token-absolute-ttl-minutes:10080}") long absoluteTtlMinutes,
            @Value("${jwt.refresh-token-idle-ttl-minutes:1440}") long idleTtlMinutes) {
        this.userSessionRepository = userSessionRepository;
        this.absoluteTtlMinutes = absoluteTtlMinutes;
        this.idleTtlMinutes = idleTtlMinutes;
    }

    @Override
    public RefreshTokenIssueResult issueRefreshToken(User user) {
        OffsetDateTime now = OffsetDateTime.now();
        String rawToken = generateRefreshToken();

        UserSession session = new UserSession();
        session.setUser(user);
        session.setRefreshTokenHash(hashToken(rawToken));
        session.setCreatedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAtAbsolute(now.plusMinutes(absoluteTtlMinutes));
        session.setExpiresAtIdle(now.plusMinutes(idleTtlMinutes));
        userSessionRepository.save(session);

        return new RefreshTokenIssueResult(session.getSessionId(), rawToken, session.getExpiresAtAbsolute());
    }

    @Override
    public RefreshTokenRotationResult rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        UserSession session = userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        OffsetDateTime now = OffsetDateTime.now();

        if (session.getExpiresAtAbsolute().isBefore(now) || session.getExpiresAtIdle().isBefore(now)) {
            session.setRevokedAt(now);
            userSessionRepository.save(session);
            throw new IllegalArgumentException("Refresh token expired");
        }

        String newToken = generateRefreshToken();
        session.setRefreshTokenHash(hashToken(newToken));
        session.setLastUsedAt(now);
        session.setExpiresAtIdle(now.plusMinutes(idleTtlMinutes));
        userSessionRepository.save(session);

        return new RefreshTokenRotationResult(session.getSessionId(), session.getUser().getId(), newToken, session.getExpiresAtAbsolute());
    }

    @Override
    public void revokeRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        String tokenHash = hashToken(rawRefreshToken);
        userSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(tokenHash)
                .ifPresent(session -> {
                    session.setRevokedAt(OffsetDateTime.now());
                    userSessionRepository.save(session);
                });
    }

    @Override
    public void revokeAllSessions(UUID userId) {
        OffsetDateTime now = OffsetDateTime.now();
        int revoked = userSessionRepository.revokeAllByUserId(userId, now);
        log.info("Revoked {} sessions for user {}", revoked, userId);
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }
}
