package com.dmdr.personal.portal.users.service;

import com.dmdr.personal.portal.users.model.User;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface RefreshTokenService {

    RefreshTokenIssueResult issueRefreshToken(User user);

    RefreshTokenRotationResult rotateRefreshToken(String rawRefreshToken);

    void revokeRefreshToken(String rawRefreshToken);

    void revokeAllSessions(UUID userId);

    record RefreshTokenIssueResult(UUID sessionId, String refreshToken, OffsetDateTime expiresAtAbsolute) {
    }

    record RefreshTokenRotationResult(UUID sessionId, UUID userId, String refreshToken, OffsetDateTime expiresAtAbsolute) {
    }
}
