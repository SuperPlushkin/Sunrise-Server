package com.Sunrise.Services.DataServices;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class LockService {

    private static final int LOCK_TIMEOUT_SECONDS = 5;
    private static final int LOCK_EXPIRATION_MILLIS = 30 * 60 * 1000;
    private static final int LOCK_CLEANUP_SCHEDULE_MILLIS = 5 * 60 * 1000;
    private static class MyReadWriteLock {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        volatile long lastAccess;

        public boolean tryLockForRead() throws InterruptedException {
            if (lock.readLock().tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
                lastAccess = System.currentTimeMillis();
                return true;
            }
            return false;
        }
        public boolean tryLockForWrite() throws InterruptedException {
            if (lock.writeLock().tryLock(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)){
                lastAccess = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        public void unLockForRead() {
            if (lock.getReadHoldCount() > 0) {
                lock.readLock().unlock();
                lastAccess = System.currentTimeMillis();
            }
        }
        public void unLockForWrite() {
            if (lock.isWriteLockedByCurrentThread()){
                lock.writeLock().unlock();
                lastAccess = System.currentTimeMillis();
            }
        }
    }
    private static class MyLock {
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

    //  ВСЕГДА в таком порядке:
    // registrationLock
    // userProfileLock
    // chatDataLock

    private final Map<Long, MyReadWriteLock> userProfileLocks = new ConcurrentHashMap<>();
    private final Map<Long, MyReadWriteLock> chatDataLocks = new ConcurrentHashMap<>();
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

    private <K> boolean tryReadLock(Map<K, MyReadWriteLock> lockMap, K key) {
        MyReadWriteLock entry = lockMap.computeIfAbsent(key, k -> new MyReadWriteLock());
        try {
            if (entry.tryLockForRead()) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    private <K> void unLockRead(Map<K, MyReadWriteLock> lockMap, K key) {
        if (lockMap.get(key) instanceof MyReadWriteLock lock) {
            lock.unLockForRead();
        }
    }

    private <K> boolean tryWriteLock(Map<K, MyReadWriteLock> lockMap, K key) {
        MyReadWriteLock entry = lockMap.computeIfAbsent(key, k -> new MyReadWriteLock());
        try {
            if (entry.tryLockForWrite()) {
                return true;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    private <K> void unLockWrite(Map<K, MyReadWriteLock> lockMap, K key) {
        if (lockMap.get(key) instanceof MyReadWriteLock lock) {
            lock.unLockForWrite();
        }
    }

    private <T> boolean tryMultipleLocks(Map<T, MyReadWriteLock> lockMap, Collection<T> keys, boolean isWrite) {
        List<T> sorted = new ArrayList<>(keys);
        sorted.sort((a, b) -> {
            if (a instanceof Comparable && b instanceof Comparable) {
                return ((Comparable) a).compareTo(b);
            }
            return a.toString().compareTo(b.toString());
        });

        List<MyReadWriteLock> acquired = new ArrayList<>();
        try {
            for (T key : sorted) {
                MyReadWriteLock entry = lockMap.computeIfAbsent(key, t -> new MyReadWriteLock());
                if (isWrite) {
                    if (!entry.tryLockForWrite()) {
                        throw new InterruptedException("Failed to lock");
                    }
                }
                else{
                    if (!entry.tryLockForRead()) {
                        throw new InterruptedException("Failed to lock");
                    }
                }
                acquired.add(entry);
            }
        }
        catch (InterruptedException ex){
            for (MyReadWriteLock le : acquired) {
                if (isWrite) {
                    le.unLockForWrite();
                } else {
                    le.unLockForRead();
                }
            }
            return false;
        }
        return true;
    }
    private <K> void unLockMultiple(Map<K, MyReadWriteLock> lockMap, Collection<K> keys, boolean isWrite) {
        List<K> sorted = new ArrayList<>(keys);
        sorted.sort((a, b) -> {
            if (a instanceof Comparable && b instanceof Comparable) {
                return ((Comparable) b).compareTo(a); // обратный порядок
            }
            return b.toString().compareTo(a.toString());
        });

        for (K key : sorted) {
            if (lockMap.get(key) instanceof MyReadWriteLock RWLock) {
                if (isWrite) {
                    RWLock.unLockForWrite();
                } else {
                    RWLock.unLockForRead();
                }
            }
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


    // ========== USER PROFILE LOCKS ==========

    public boolean tryLockUserRead(long userId) {
        return tryReadLock(userProfileLocks, userId);
    }
    public void unLockUserRead(long userId) {
        unLockRead(userProfileLocks, userId);
    }

    public boolean tryLockUserProfileForWrite(long userId) {
        return tryWriteLock(userProfileLocks, userId);
    }
    public void unLockUserProfileForWrite(long userId) {
        unLockWrite(userProfileLocks, userId);
    }


    // ========== CHAT DATA LOCKS ==========

    public boolean tryLockChat(long chatId, boolean lockChatForWrite) {
        return lockChatForWrite ? tryWriteLock(chatDataLocks, chatId) : tryReadLock(chatDataLocks, chatId);
    }
    public void unLockChat(long chatId, boolean wasChatLockedForWrite) {
        if (wasChatLockedForWrite) {
            unLockWrite(chatDataLocks, chatId);
        } else {
            unLockRead(chatDataLocks, chatId);
        }
    }


    // ========== MULTI-LOCK METHODS FOR CHAT-AND-USER ==========

    public boolean tryLockChatAndUser(long chatId, long userId, boolean lockChatForWrite, boolean lockUserForWrite) {

        boolean userLocked = lockUserForWrite
                ? tryLockUserProfileForWrite(userId)
                : tryLockUserRead(userId);

        if (!userLocked) {
            return false;
        }

        boolean chatLocked = tryLockChat(chatId, lockChatForWrite);
        if (!chatLocked) {
            if(lockUserForWrite){
                unLockUserProfileForWrite(userId);
            } else{
                unLockUserRead(userId);
            }
        }
        return chatLocked;
    }
    public void unLockChatAndUser(long chatId, long userId, boolean wasChatLockedForWrite, boolean wereUsersLockedForWrite) {
        // обратный порядок
        unLockChat(chatId, wasChatLockedForWrite);
        if (wereUsersLockedForWrite){
            unLockUserProfileForWrite(userId);
        } else{
            unLockUserRead(userId);
        }
    }

    public boolean tryLockChatWriteUserRead(long chatId, long userId) {
        return tryLockChatAndUser(chatId, userId, true, false);
    }
    public void unLockChatWriteUserRead(long chatId, long userId) {
        unLockChatAndUser(chatId, userId, true, false);
    }

    public boolean tryLockChatReadUserRead(long chatId, long userId) {
        return tryLockChatAndUser(chatId, userId, false, false);
    }
    public void unLockChatReadUserRead(long chatId, long userId) {
        unLockChatAndUser(chatId, userId, false, false);
    }


    // ========== MULTI-LOCK METHODS FOR CHAT-AND-USERS ==========

    public boolean tryLockChatAndUsers(long chatId, Collection<Long> userIds, boolean lockChatForWrite, boolean lockUsersForWrite) {
        if (!tryMultipleLocks(userProfileLocks, userIds, lockUsersForWrite))
            return false;

        if (!tryLockChat(chatId, lockChatForWrite)) {
            unLockMultiple(userProfileLocks, userIds, lockUsersForWrite);
            return false;
        }
        return true;
    }
    public void unLockChatAndUsers(long chatId, Collection<Long> userIds, boolean wasChatLockedForWrite, boolean wereUsersLockedForWrite) {
        // обратный порядок
        unLockChat(chatId, wasChatLockedForWrite);
        unLockMultiple(userProfileLocks, userIds, wereUsersLockedForWrite);
    }

    public boolean tryLockChatWriteUsersRead(long chatId, Collection<Long> userIds) {
        return tryLockChatAndUsers(chatId, userIds, true, false);
    }
    public void unLockChatWriteUsersRead(long chatId, Collection<Long> userIds) {
        unLockChatAndUsers(chatId, userIds, true, false);
    }


    // ========== STATS ==========

    public Map<String, Object> getLockStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userProfileLocks.size", userProfileLocks.size());
        stats.put("chatDataLocks.size", chatDataLocks.size());
        stats.put("registrationLocks.size", registrationLocks.size());

        stats.put("userProfileLocks.active", countActiveLocks(userProfileLocks));
        stats.put("chatDataLocks.active", countActiveLocks(chatDataLocks));

        return stats;
    }
    private int countActiveLocks(Map<?, MyReadWriteLock> lockMap) {
        return (int) lockMap.values().stream()
                .filter(e -> e.lock.isWriteLocked() || e.lock.getReadLockCount() > 0)
                .count();
    }


    // ========== CLEANUP ==========

    @Scheduled(fixedDelay = LOCK_CLEANUP_SCHEDULE_MILLIS)
    public void cleanupOldLocks() {
        long now = System.currentTimeMillis();
        cleanupReadWriteLockMap(userProfileLocks, now);
        cleanupReadWriteLockMap(chatDataLocks, now);
        cleanupLockMap(registrationLocks, now);
    }
    private void cleanupLockMap(Map<?, MyLock> lockMap, long now) {
        lockMap.entrySet().removeIf(entry -> {
            MyLock lock = entry.getValue();
            return !lock.lock.isLocked() && now - lock.lastAccess > LOCK_EXPIRATION_MILLIS;
        });
    }
    private void cleanupReadWriteLockMap(Map<?, MyReadWriteLock> lockMap, long now) {
        lockMap.entrySet().removeIf(entry -> {
            MyReadWriteLock lock = entry.getValue();
            return !lock.lock.isWriteLocked() &&
                    lock.lock.getReadLockCount() == 0 &&
                    now - lock.lastAccess > LOCK_EXPIRATION_MILLIS;
        });
    }
}