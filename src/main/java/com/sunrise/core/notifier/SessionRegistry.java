package com.sunrise.core.notifier;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionRegistry {
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>(); // sessionId -> userId
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>(); // userId -> Set<sessionId>

    public void register(String sessionId, Long userId) {
        sessionToUser.put(sessionId, userId);
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }
    public void unregister(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
    }

    public Long getUserId(String sessionId) {
        return sessionToUser.get(sessionId);
    }
    public Set<String> getUserSessions(Long userId) {
        return userSessions.getOrDefault(userId, Collections.emptySet());
    }
}