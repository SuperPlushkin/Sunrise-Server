package com.Sunrise.Entities.Cache;

import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean isChatsIdsFullyLoaded = new AtomicBoolean(false);

    public boolean isChatsIdsFullyLoaded() {
        return isChatsIdsFullyLoaded.get();
    }
    public Set<Long> getChatsIds() {
        return Set.copyOf(chatsIds);
    }
    public SimpleEntry<Set<Long>, Boolean> getChatsIdsAndIsFullyLoaded() {
        return new SimpleEntry<>(new HashSet<>(chatsIds), isChatsIdsFullyLoaded.get());
    }
    public boolean hasChat(Long chatId) {
        return chatsIds.contains(chatId);
    }
    public int getChatsCount() {
        return chatsIds.size();
    }

    public void setChatsIds(Set<Long> chatsIds, boolean isFullyLoaded){
        this.chatsIds.clear();
        this.chatsIds.addAll(chatsIds);
        this.isChatsIdsFullyLoaded.set(isFullyLoaded);
    }
    public void addChats(Set<Long> newChatsIds, boolean isFullyLoaded) {
        for (Long chatId : newChatsIds) {
            chatsIds.add(chatId);  // атомарно по одному
        }
        isChatsIdsFullyLoaded.set(isFullyLoaded);
    }
    public void addChat(Long chatId) {
        chatsIds.add(chatId);
        isChatsIdsFullyLoaded.compareAndSet(true, false);
    }
    public void removeChat(Long chatId) {
        if (chatsIds.remove(chatId)) {
            isChatsIdsFullyLoaded.compareAndSet(true, false);
        }
    }
    public void clearChats(boolean isFullyLoaded) {
        chatsIds.clear();
        isChatsIdsFullyLoaded.set(isFullyLoaded);
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
