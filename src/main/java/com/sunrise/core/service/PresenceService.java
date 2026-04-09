package com.sunrise.core.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceService {
    private final Map<Long, String> userStatus = new ConcurrentHashMap<>(); // userId -> status
    private final Map<Long, Map<Long, Boolean>> typingUsers = new ConcurrentHashMap<>(); // chatId -> (userId -> typing)

    public void setStatus(Long userId, String status) {
        userStatus.put(userId, status);
    }
    public String getStatus(Long userId) {
        return userStatus.getOrDefault(userId, "offline");
    }

    public void setTyping(Long userId, Long chatId, boolean typing) {
        typingUsers.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>()).put(userId, typing);
        // optional: schedule removal after timeout
    }
    public boolean isTyping(Long userId, Long chatId) {
        Map<Long, Boolean> chatTyping = typingUsers.get(chatId);
        return chatTyping != null && chatTyping.getOrDefault(userId, false);
    }

    public void removeUserCompletely(Long userId) {
        userStatus.remove(userId);
        typingUsers.values().forEach(map -> map.remove(userId));
    }
}