package com.Sunrise.Services.DataServices.CacheEntities;

import com.Sunrise.Entities.User;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CacheUser extends User {

    private final Set<Long> chatsCache = ConcurrentHashMap.newKeySet(); // chatId

    public CacheUser(User user) {
        super(user.getId(), user.getUsername(), user.getName(), user.getEmail(), user.getHashPassword(), null, user.getCreatedAt(), user.getIsEnabled(), user.getIsDeleted());
    }

    public void addChat(Long chatId) {
        chatsCache.add(chatId);
    }
    public void removeChat(Long chatId) {
        chatsCache.remove(chatId);
    }
    public boolean hasChat(Long chatId) {
        return chatsCache.contains(chatId);
    }
    public Set<Long> getChats() {
        return Set.copyOf(chatsCache);
    } // Возвращаем копию
    public int getChatsCount() {
        return chatsCache.size();
    }
    public void clearChats() {
        chatsCache.clear();
    }
}
