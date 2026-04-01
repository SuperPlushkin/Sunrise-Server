package com.Sunrise.Core.DataServices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LockManager {

    @Value("${app.locks.timeout}")
    private int LOCK_TIMEOUT_SECONDS;
    @Value("${app.locks.clean-up-expiration}")
    private int LOCK_EXPIRATION_MILLIS;

    private class MyLock {
        private final ReentrantLock lock = new ReentrantLock();
        volatile long lastAccess;

        public boolean tryLock() throws InterruptedException {
            if (lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
                lastAccess = System.currentTimeMillis();
                return true;
            }
            return false;
        }
        public void unLock() {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                lastAccess = System.currentTimeMillis();
            }
        }
    }


    // ========== LOCK MAPS ==========

    private final Map<Long, MyLock> chatDataLocks = new ConcurrentHashMap<>();
    private final Map<String, MyLock> registrationLocks = new ConcurrentHashMap<>();


    // ========== ABSTRACT LOCK FUNCTIONS ==========

    private <K> boolean tryLock(Map<K, MyLock> lockMap, K key) {
        MyLock entry = lockMap.computeIfAbsent(key, k -> new MyLock());
        try {
            if (entry.tryLock()) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    private <K> void unLock(Map<K, MyLock> lockMap, K key) {
        if (lockMap.get(key) instanceof MyLock lock) {
            lock.unLock();
        }
    }


    // ========== REGISTRATION LOCKS ==========

    public boolean tryLockRegistration(String username, String email) {
        String key = "username:" + username.toLowerCase().trim() + ":email:" + email.toLowerCase().trim();
        return tryLock(registrationLocks, key);
    }
    public void unLockRegistration(String username, String email) {
        String key = "username:" + username.toLowerCase().trim() + ":email:" + email.toLowerCase().trim();
        unLock(registrationLocks, key);
    }


    // ========== CHAT DATA LOCKS ==========

    public boolean tryLockChatWrite(long chatId) {
        return tryLock(chatDataLocks, chatId);
    }
    public void unLockChatWrite(long chatId) {
        unLock(chatDataLocks, chatId);
    }


    // ========== STATS ==========

    public Map<String, Object> getLockStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("chatDataLocks.size", chatDataLocks.size());
        stats.put("registrationLocks.size", registrationLocks.size());
        return stats;
    }


    // ========== CLEANUP ==========

    @Scheduled(fixedDelayString = "${app.locks.clean-up-schedule}")
    public void cleanupOldLocks() {
        long now = System.currentTimeMillis();
        cleanupLockMap(chatDataLocks, now);
        cleanupLockMap(registrationLocks, now);
    }
    private void cleanupLockMap(Map<?, MyLock> lockMap, long now) {
        lockMap.entrySet().removeIf(entry -> {
            MyLock lock = entry.getValue();
            return !lock.lock.isLocked() && now - lock.lastAccess > LOCK_EXPIRATION_MILLIS;
        });
    }
}