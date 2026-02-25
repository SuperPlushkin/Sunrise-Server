package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.User;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CacheUser extends User {

    private final Set<Long> chatsIds = ConcurrentHashMap.newKeySet(); // chatId

    @lombok.Getter
    @lombok.Setter
    private boolean isChatsIdsFullyLoaded = false;

    public CacheUser(User user) {
        super(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getHashPassword(), null, user.getCreatedAt(), user.isEnabled(), user.isDeleted());
    }

    public Set<Long> getChatsIds() {
        return Set.copyOf(chatsIds);
    }
    public SimpleEntry<Set<Long>, Boolean> getChatsIdsAndIsFullyLoaded() {
        return new SimpleEntry<>(new HashSet<>(chatsIds), isChatsIdsFullyLoaded);
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
        this.isChatsIdsFullyLoaded = isFullyLoaded;
    }
    public void addChats(Set<Long> newChatsIds, boolean isFullyLoaded) {
        chatsIds.addAll(newChatsIds);
        this.isChatsIdsFullyLoaded = isFullyLoaded;
    }
    public void addChat(Long chatId) {
        chatsIds.add(chatId);
    }
    public void removeChat(Long chatId) {
        chatsIds.remove(chatId);
    }
    public void clearChats(boolean isFullyLoaded) {
        chatsIds.clear();
        this.isChatsIdsFullyLoaded = isFullyLoaded;
    }

    public void updateFromEntity(User user) {
        this.setUsername(user.getUsername());
        this.setName(user.getName());
        this.setEmail(user.getEmail());
        this.setHashPassword(user.getHashPassword());
        this.setLastLogin(user.getLastLogin());
        this.setEnabled(user.isEnabled());
        this.setDeleted(user.isDeleted());
    }
}
