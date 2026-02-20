package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.User;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CacheUser extends User {

    private final Set<Long> chatsIds = ConcurrentHashMap.newKeySet(); // chatId

    public CacheUser(User user) {
        super(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getHashPassword(), null, user.getCreatedAt(), user.getIsEnabled(), user.getIsDeleted());
    }

    public Set<Long> getChats() {
        return Set.copyOf(chatsIds);
    }
    public int getChatsCount() {
        return chatsIds.size();
    }

    public void addChat(Long chatId) {
        chatsIds.add(chatId);
    }
    public void removeChat(Long chatId) {
        chatsIds.remove(chatId);
    }
    public boolean hasChat(Long chatId) {
        return chatsIds.contains(chatId);
    }
    public void clearChats() {
        chatsIds.clear();
    }

    public void setChatsIds(Set<Long> newChatsIds) {
        chatsIds.clear();
        chatsIds.addAll(newChatsIds);
    }
    public void updateFromEntity(User user) {
        this.setUsername(user.getUsername());
        this.setName(user.getName());
        this.setEmail(user.getEmail());
        this.setHashPassword(user.getHashPassword());
        this.setLastLogin(user.getLastLogin());
        this.setIsEnabled(user.getIsEnabled());
        this.setIsDeleted(user.getIsDeleted());
    }
}
