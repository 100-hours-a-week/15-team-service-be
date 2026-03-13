package com.sipomeokjo.commitme.domain.refreshToken.service;

import com.sipomeokjo.commitme.domain.user.entity.UserStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class RefreshTokenCacheAfterCommitService {

    private final RefreshTokenCacheService refreshTokenCacheService;

    public void cache(String tokenHash, Long userId, UserStatus status, Instant expiresAt) {
        executeAfterCommit(
                () -> refreshTokenCacheService.cache(tokenHash, userId, status, expiresAt));
    }

    public void evict(String tokenHash) {
        executeAfterCommit(() -> refreshTokenCacheService.evict(tokenHash));
    }

    public void evictAll(Collection<String> tokenHashes) {
        if (tokenHashes == null || tokenHashes.isEmpty()) {
            return;
        }
        List<String> copiedTokenHashes = List.copyOf(tokenHashes);
        executeAfterCommit(() -> refreshTokenCacheService.evictAll(copiedTokenHashes));
    }

    private void executeAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
            return;
        }
        action.run();
    }
}
