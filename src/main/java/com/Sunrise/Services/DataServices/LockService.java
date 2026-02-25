package com.Sunrise.Services.DataServices;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class LockService {

    // TODO: Заменить на Redis при масштабировании
    private static final int LOCK_TIMEOUT_SECONDS = 5;
    private static final int LOCK_EXPIRATION_MILLIS = 30 * 60 * 1000;
    private static final int LOCK_CLEANUP_SCHEDULE_MILLIS = 5 * 60 * 1000;


    // Основные локи для блокировки
    private final Map<Long, ReentrantLock> chatLocks = new ConcurrentHashMap<>(); // chatId -> Lock
    private final Map<Long, ReentrantLock> userWriteLocks = new ConcurrentHashMap<>(); // userId -> Lock
    private final Map<String, ReentrantLock> registrationLocks = new ConcurrentHashMap<>(); // "username:email" -> Lock


    // Счетчик использований для очистки
    private final Map<Long, Long> chatLockLastAccess = new ConcurrentHashMap<>();
    private final Map<Long, Long> userLockLastAccess = new ConcurrentHashMap<>();
    private final Map<String, Long> registrationLockLastAccess = new ConcurrentHashMap<>();


    // ========== REGISTRATION LOCKS ==========

    public boolean tryLockRegistration(String username, String email) {
        String key = "username:" + username.toLowerCase().trim() + "email:" + email.toLowerCase().trim();
        return tryLockWithTrackingAccess(key, registrationLocks, registrationLockLastAccess);
    }
    public void unlockRegistration(String username, String email) {
        String key = "username:" + username.toLowerCase().trim() + "email:" + email.toLowerCase().trim();
        unLockWithTrackingAccess(key, registrationLocks, registrationLockLastAccess);
    }


    // ========== CHAT LOCKS ==========

    public boolean tryLockChat(Long chatId) {
        return tryLockWithTrackingAccess(chatId, chatLocks, chatLockLastAccess);
    }
    public void unlockChat(Long chatId) {
        unLockWithTrackingAccess(chatId, chatLocks, chatLockLastAccess);
    }


    // ========== USER LOCKS ==========

    public boolean tryLockUser(Long userId) {
        return tryLockWithTrackingAccess(userId, userWriteLocks, userLockLastAccess);
    }
    public void unlockUser(Long userId) {
        unLockWithTrackingAccess(userId, userWriteLocks, userLockLastAccess);
    }


    // для нескольких user
    public boolean tryLockUsers(Collection<Long> userIds) {
        List<Long> sorted = new ArrayList<>(userIds);
        sorted.sort(Long::compareTo); // deadlock farewell

        List<Long> acquiredLocks = new ArrayList<>();
        for (Long userId : sorted) {
            if (!tryLockWithTrackingAccess(userId, userWriteLocks, userLockLastAccess)) {
                // не удалось захватить, придется освобождать
                acquiredLocks.forEach(this::unlockUser);
                return false;
            }
            acquiredLocks.add(userId);
        }
        return true;
    }
    public void unlockUsers(Collection<Long> userIds) {
        List<Long> sorted = new ArrayList<>(userIds);
        sorted.sort(Collections.reverseOrder()); // deadlock farewell

        for (Long userId : sorted) {
            unLockWithTrackingAccess(userId, userWriteLocks, userLockLastAccess);
        }
    }


    // ========== HELPER FUNCTIONS ==========

    private boolean tryLock(ReentrantLock lock) {
        try {
            return lock.tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    private <T> boolean tryLockWithTrackingAccess(T key, Map<T, ReentrantLock> lockMap, Map<T, Long> accessMap) {
        ReentrantLock lock = lockMap.get(key);
        if (tryLock(lock)) {
            accessMap.put(key, System.currentTimeMillis());
            return true;
        }
        return false;
    }
    private <T> void unLockWithTrackingAccess(T key, Map<T, ReentrantLock> lockMap, Map<T, Long> accessMap) {
        ReentrantLock lock = lockMap.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
            accessMap.put(key, System.currentTimeMillis());
        }
    }
    public Map<String, Object> getLockStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("chatLocks.size", chatLocks.size());
        stats.put("userLocks.size", userWriteLocks.size());
        stats.put("registrationLocks.size", registrationLocks.size());
        return stats;
    }


    // ========== CLEAN-UP FUNCTIONS ==========

    @Scheduled(fixedDelay = LOCK_CLEANUP_SCHEDULE_MILLIS)
    public void cleanupOldLocks() {
        long now = System.currentTimeMillis();

        // Очистка chat locks
        chatLockLastAccess.entrySet().removeIf(entry ->
                cleanupLock(entry.getKey(), entry.getValue(), chatLocks, now));

        // Очистка user locks
        userLockLastAccess.entrySet().removeIf(entry ->
                cleanupLock(entry.getKey(), entry.getValue(), userWriteLocks, now));

        // Очистка registration locks
        registrationLockLastAccess.entrySet().removeIf(entry ->
                cleanupLock(entry.getKey(), entry.getValue(), registrationLocks, now));
    }
    private <T> boolean cleanupLock(T key, Long lastAccess, Map<T, ReentrantLock> lockMap, long now) {
        if (now - lastAccess > LOCK_EXPIRATION_MILLIS) {
            ReentrantLock lock = lockMap.get(key);
            if (lock != null && !lock.isLocked()) {
                lockMap.remove(key);
                return true;
            }
        }
        return false;
    }
}