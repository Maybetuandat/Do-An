package com.example.be.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabCleanupScheduler {

    private final LabService labService;

    /**
     * Cleanup expired labs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes in milliseconds
    public void cleanupExpiredLabs() {
        try {
            log.debug("Running scheduled cleanup of expired labs...");
            labService.cleanupExpiredLabs();
        } catch (Exception e) {
            log.error("Error during scheduled lab cleanup: {}", e.getMessage(), e);
        }
    }
}