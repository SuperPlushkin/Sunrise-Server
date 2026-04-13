package com.sunrise.core.service;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceService {

    private final Map<Long, Set<String>> watchers = new ConcurrentHashMap<>(); // userId -> Set<sessionId>
    private final Map<Long, String> userOnlineStatus = new ConcurrentHashMap<>(); // userId -> status

    private final Map<Long, Set<Long>> chatToUsersActionStatus = new ConcurrentHashMap<>(); // chatId -> Set<userId>
    private final Map<Long, String> userActionStatusToChat = new ConcurrentHashMap<>(); // userId -> typingStatus


    // ==================== Онлайн статусы ====================

    public Set<String> updateUserOnlineStatus(long userId, String status) {
        String previousStatus = userOnlineStatus.compute(userId, (id, current) -> status.equals(current) ? current : status);
        if (previousStatus.equals(status)) {
            return Collections.emptySet();
        }

        return Set.copyOf(watchers.getOrDefault(userId, Collections.emptySet()));
    }
    public String getUserOnlineStatus(long userId) {
        return userOnlineStatus.getOrDefault(userId, "offline");
    }


    // ==================== Подписки на статусы ====================

    public void subscribeUserStatus(long targetUserId, String sessionId) {
        watchers.computeIfAbsent(targetUserId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }
    public void unsubscribeUserStatus(long targetUserId, String sessionId) {
        Set<String> sessions = watchers.get(targetUserId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                watchers.remove(targetUserId);
            }
        }
    }


    // ==================== Действия в чате ====================

    public boolean updateUserAction(long userId, long chatId, String actionStatus) {
        String currentAction = userActionStatusToChat.get(userId);
        if (actionStatus.equals("none")) { // Остановка действия
            if (currentAction == null) return false;
            userActionStatusToChat.remove(userId);
            removeUserFromChatActions(userId, chatId);
            return true;
        } else if (actionStatus.equals(currentAction)) { // Начало или смена действия
            return false;
        }

        // Новое действие
        userActionStatusToChat.put(userId, actionStatus);
        chatToUsersActionStatus.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        return true;
    }
    public Map<Long, String> getUsersActionsInChat(long chatId) {
        Set<Long> userIds = chatToUsersActionStatus.getOrDefault(chatId, Collections.emptySet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, String> result = new HashMap<>();
        for (Long userId : userIds) {
            String action = userActionStatusToChat.get(userId);
            if (action != null) {
                result.put(userId, action);
            }
        }
        return result;
    }
    private void removeUserFromChatActions(long userId, long chatId) {
        Set<Long> users = chatToUsersActionStatus.get(chatId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                chatToUsersActionStatus.remove(chatId);
            }
        }
    }


    // ==================== Очистка ====================

    public void cleanupSession(String sessionId) {
        if (sessionId == null) return;

        watchers.values().forEach(sessions -> sessions.remove(sessionId));
        watchers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
    public void removeUserCompletely(long userId) {
        userOnlineStatus.remove(userId);
        watchers.remove(userId);

        // Очищаем действия пользователя
        userActionStatusToChat.remove(userId);
        for (Map.Entry<Long, Set<Long>> entry : chatToUsersActionStatus.entrySet()) {
            if (entry.getValue().remove(userId)) {
                if (entry.getValue().isEmpty()) {
                    chatToUsersActionStatus.remove(entry.getKey());
                }
                break;
            }
        }
    }
}