package com.dmdr.personal.portal.users.service.impl;

import com.dmdr.personal.portal.users.repository.UserSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@Slf4j
public class RefreshTokenCleanupService {

    private final UserSessionRepository userSessionRepository;

    public RefreshTokenCleanupService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Scheduled(fixedDelayString = "${jwt.refresh-token-cleanup-interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredSessions() {
        int deleted = userSessionRepository.deleteExpiredOrRevoked(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Deleted {} expired or revoked user sessions", deleted);
        }
    }
}
