package com.Sunrise.Services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class LockService {

    private final ReentrantLock globalChatsLock = new ReentrantLock();
    private final ReentrantLock authLock = new ReentrantLock();

    private final Map<Long, ReadWriteLock> chatSpecificLocks = new ConcurrentHashMap<>();
    private final Map<Long, ReadWriteLock> userSpecificLocks = new ConcurrentHashMap<>();


    // ========== GLOBAL LOCKS ==========

    public void lockGlobalChats() {
        globalChatsLock.lock();
    }
    public void unlockGlobalChats() {
        globalChatsLock.unlock();
    }


    // ========== CHAT LOCKS ==========

    public Lock getReadChatLock(Long chatId) {
        return chatSpecificLocks.computeIfAbsent(chatId, k -> new ReentrantReadWriteLock()).readLock();
    }
    public Lock getWriteChatLock(Long chatId) {
        return chatSpecificLocks.computeIfAbsent(chatId, k -> new ReentrantReadWriteLock()).writeLock();
    }

    public void lockReadChat(Long chatId) {
        getReadChatLock(chatId).lock();
    }
    public void unlockReadChat(Long chatId) {
        getReadChatLock(chatId).unlock();
    }

    public void lockWriteChat(Long chatId) {
        getWriteChatLock(chatId).lock();
    }
    public void unlockWriteChat(Long chatId) {
        getWriteChatLock(chatId).unlock();
    }


    // ========== USER LOCKS ==========

    public Lock getReadUserLock(Long userId) {
        return userSpecificLocks.computeIfAbsent(userId, k -> new ReentrantReadWriteLock()).readLock();
    }
    public Lock getWriteUserLock(Long userId) {
        return userSpecificLocks.computeIfAbsent(userId, k -> new ReentrantReadWriteLock()).writeLock();
    }

    public void lockReadUser(Long userId) {
        getReadUserLock(userId).lock();
    }
    public void unlockReadUser(Long userId) {
        getReadUserLock(userId).unlock();
    }

    public void lockWriteUser(Long userId) {
        getWriteUserLock(userId).lock();
    }
    public void unlockWriteUser(Long userId) {
        getWriteUserLock(userId).unlock();
    }


    // ========== BATCH OPERATIONS ==========

    public void lockMultipleUsers(Set<Long> userIds, boolean writeLock) {
        userIds.stream().sorted()
                .forEach(userId -> {
                    if (writeLock)
                    {
                        lockWriteUser(userId);
                    }
                    else lockReadUser(userId);
                });
    }
    public void unlockMultipleUsers(Set<Long> userIds, boolean writeLock) {
        userIds.forEach(userId -> {
            if (writeLock) unlockWriteUser(userId);
            else unlockReadUser(userId);
        });
    }

    public void lockUsersAndChat(Long chatId, Set<Long> userIds, boolean usersWriteLock) {
        lockMultipleUsers(userIds, usersWriteLock);
        lockWriteChat(chatId);
    }
    public void unlockUsersAndChat(Long chatId, Set<Long> userIds, boolean usersWriteLock) {
        unlockWriteChat(chatId);
        unlockMultipleUsers(userIds, usersWriteLock);
    }


    // ========== ORDERED LOCKING (Deadlock prevention) ==========

    public void acquireLocksInOrder(Lock... locks) {
        List<Lock> sortedLocks = Arrays.stream(locks).sorted(Comparator.comparing(System::identityHashCode)).toList();

        for (Lock lock : sortedLocks) {
            lock.lock();
        }
    }
    public void releaseLocks(Lock... locks) {
        for (Lock lock : locks)
            lock.unlock();
    }
    public Lock[] getOrderedUserLocks(Set<Long> userIds, boolean writeLock) {
        return userIds.stream().sorted()
                .map(userId -> writeLock ? getWriteUserLock(userId) : getReadUserLock(userId))
                .toArray(Lock[]::new);
    }


    // ========== LOCK MANAGEMENT ==========

    @Scheduled(fixedRate = 300000) // Каждые 5 минут
    public void cleanupExpiredLocks() {
        globalChatsLock.lock();
        try {
            // Здесь нужно добавить логику проверки существования чатов/пользователей
//             chatSpecificLocks.entrySet().removeIf(entry -> !chatExists(entry.getKey()));
//             userSpecificLocks.entrySet().removeIf(entry -> !userExists(entry.getKey()));
        } finally {
            globalChatsLock.unlock();
        }
    }

    public LockStats getLockStats() {
        return new LockStats(
            chatSpecificLocks.size(),
            userSpecificLocks.size(),
            globalChatsLock.getQueueLength()
        );
    }
    public record LockStats(int chatLocksCount, int userLocksCount, int globalLockQueueLength) {}
}