package com.Sunrise.Services;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class LockService {

    // TODO: Заменить на Redis при масштабировании
    // В данный момент работает для single-instance deployment

    private final ReentrantLock globalChatsLock = new ReentrantLock();

    private final Map<Long, ReadWriteLock> chatSpecificLocks = new ConcurrentHashMap<>();
    private final Map<Long, ReadWriteLock> userSpecificLocks = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> registrationLocks = new ConcurrentHashMap<>();


    // ========== GLOBAL LOCKS ==========

    public void lockGlobalChats() {
        globalChatsLock.lock();
    }
    public void unlockGlobalChats() {
        globalChatsLock.unlock();
    }


    // ========== REGISTRATION LOCKS ==========

    public boolean lockRegistration(String username, String email) {
        String userKey = "username:" + username.trim();
        String emailKey = "email:" + email.trim();

        try {
            ReentrantLock lock1 = registrationLocks.computeIfAbsent(userKey, k -> new ReentrantLock());
            if (!lock1.tryLock(2, TimeUnit.SECONDS)) return false;

            ReentrantLock lock2 = registrationLocks.computeIfAbsent(emailKey, k -> new ReentrantLock());
            if (!lock2.tryLock(2, TimeUnit.SECONDS)) {
                lock1.unlock();
                return false;
            }

            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    public void unlockRegistration(String username, String email) {
        ReentrantLock lock2 = registrationLocks.get("email:" + email.trim());
        ReentrantLock lock1 = registrationLocks.get("username:" + username.trim());

        if (lock2 != null && lock2.isHeldByCurrentThread()) lock2.unlock();
        if (lock1 != null && lock1.isHeldByCurrentThread()) lock1.unlock();
    }


    // ========== CHAT LOCKS ==========

    public void lockReadChat(Long chatId) {
        chatSpecificLocks.computeIfAbsent(chatId, k -> new ReentrantReadWriteLock()).readLock().lock();
    }
    public void unlockReadChat(Long chatId) {
        chatSpecificLocks.computeIfAbsent(chatId, k -> new ReentrantReadWriteLock()).readLock().unlock();
    }

    public void lockWriteChat(Long chatId) {
        chatSpecificLocks.computeIfAbsent(chatId, k -> new ReentrantReadWriteLock()).writeLock().lock();
    }
    public void unlockWriteChat(Long chatId) {
        chatSpecificLocks.computeIfAbsent(chatId, k -> new ReentrantReadWriteLock()).writeLock().unlock();
    }


    // ========== USER LOCKS ==========

    private Lock getReadUserLock(Long userId) {
        return userSpecificLocks.computeIfAbsent(userId, k -> new ReentrantReadWriteLock()).readLock();
    }
    private Lock getWriteUserLock(Long userId) {
        return userSpecificLocks.computeIfAbsent(userId, k -> new ReentrantReadWriteLock()).writeLock();
    }

    public void lockUsersSafely(Set<Long> usersToLock, boolean writeLock) {
        usersToLock.stream().sorted()
                .forEach(userId -> {
                    if (writeLock)
                        getWriteUserLock(userId).lock();
                    else
                        getReadUserLock(userId).lock();
                });
    }
    public void unlockUsersSafely(Set<Long> usersToLock, boolean writeLock) {
        usersToLock.stream().sorted((a, b) -> Long.compare(b, a))
                .forEach(userId -> {
                    if (writeLock)
                        getWriteUserLock(userId).unlock();
                    else
                        getReadUserLock(userId).unlock();
                });
    }
}