package com.Sunrise.Services;

import com.Sunrise.Services.DataServices.DataAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);
    private final DataAccessService dataAccessService;

    public TokenCleanupService(DataAccessService dataAccessService) {
        this.dataAccessService = dataAccessService;
    }

    @Scheduled(initialDelay = 10000, fixedRate = 86400000) // Каждые 24 часа
    @Transactional
    public void cleanupExpiredTokens() {
        try
        {
            dataAccessService.cleanupExpiredTokensAndWait();

            log.info("✅ Expired tokens cleanup completed");
        }
        catch (Exception e)
        {
            log.error("❌ Error during token cleanup: {}", e.getMessage());
            throw new RuntimeException("Error during CleanupExpiredTokens: " + e.getMessage(), e);
        }
    }
}
