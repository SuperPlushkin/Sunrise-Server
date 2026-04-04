package com.sunrise.entity.cache;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheUser {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String hashPassword;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private boolean isEnabled;
    private boolean isDeleted;

    private final Set<Long> chatsIds = ConcurrentHashMap.newKeySet(); // chatId

    public void addChat(Long chatId) {
        chatsIds.add(chatId);
    }
    public void removeChat(Long chatId) {
        chatsIds.remove(chatId);
    }

    public boolean hasChat(Long chatId) {
        return chatsIds.contains(chatId);
    }
    public int getChatsCount() {
        return chatsIds.size();
    }
    public boolean isActive() {
        return !isDeleted;
    }
    public void updateFromCache(CacheUser cacheUser) {
        setUsername(cacheUser.getUsername());
        setName(cacheUser.getName());
        setEmail(cacheUser.getEmail());
        setHashPassword(cacheUser.getHashPassword());
        setLastLogin(cacheUser.getLastLogin());
        setEnabled(cacheUser.isEnabled());
        setDeleted(cacheUser.isDeleted());
    }
}
