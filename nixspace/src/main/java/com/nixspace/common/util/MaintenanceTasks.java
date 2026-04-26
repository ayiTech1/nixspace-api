package com.nixspace.common.util;

import com.nixspace.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Scheduled housekeeping jobs.
 * These run in the background to keep the database tidy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaintenanceTasks {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Purge expired and revoked refresh tokens daily at 2:00 AM UTC.
     * Prevents the refresh_tokens table from growing indefinitely.
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Starting scheduled purge of expired/revoked refresh tokens");
        try {
            refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
            log.info("Refresh token purge completed");
        } catch (Exception ex) {
            log.error("Refresh token purge failed: {}", ex.getMessage(), ex);
        }
    }
}
