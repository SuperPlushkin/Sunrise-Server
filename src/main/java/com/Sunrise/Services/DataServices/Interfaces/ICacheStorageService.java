package com.Sunrise.Services.DataServices.Interfaces;

import com.Sunrise.Entities.User;
import com.Sunrise.Services.DataServices.CacheService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ICacheStorageService extends IStorageService {

    // ========== CHAT MEMBER METHODS ==========

    Optional<Boolean> isGroupChat(Long chatId);
    Optional<List<Long>> getUserChats(Long userId);
    Set<Long> getChatMembers(Long chatId);
    void saveUser(User user);
    void enableUser(Long userId);
    void addUserToChat(Long userId, Long chatId);


    // ========== ADMIN RIGHTS METHODS ==========

    Set<Long> getChatAdmins(Long chatId);


    // ========== CACHE MANAGEMENT ==========

    CacheService.CacheStats getStats();
}
