package com.Sunrise.Services.DataServices;

import com.Sunrise.Entities.DB.User;
import com.Sunrise.Entities.DB.Chat;
import com.Sunrise.Entities.DB.VerificationToken;
import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import com.Sunrise.Entities.Cache.CacheUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CacheService {

    // кэш пользователей
    private final Cache<Long, CacheUser> userCache = Caffeine.newBuilder()  // userId -> CacheUser (пользователи)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS) // 1 h
            .recordStats()
            .build();

    private final Map<String, Long> usernameIndex = new ConcurrentHashMap<>(); // username -> userId (для регистрации)
    private final Map<String, Long> emailIndex = new ConcurrentHashMap<>(); // email -> userId (для регистрации)

    private final Cache<String, List<User>> searchIndex = Caffeine.newBuilder() // для getFilteredUsers (Это можно будет заменить потом на ElasticSearch)
            .maximumSize(1000)
            .expireAfterWrite(5, TimeUnit.MINUTES) // 5 min
            .build();


    // кэш чатов
    private final Cache<Long, CacheChat> chatInfoCache = Caffeine.newBuilder() // chatId -> CacheChat (чаты)
            .maximumSize(50_000)
            .expireAfterWrite(12, TimeUnit.HOURS) // 12 h
            .build();

    private final Map<String, Long> personalChatIndex = new ConcurrentHashMap<>(1000); // "creatorId:userId" -> chatId (личные чаты)


    // кеш токенов подтверждения
    private final Map<String, VerificationToken> verificationTokenCache = new ConcurrentHashMap<>(500); // token -> VerificationToken (токены)



    // ========== USER METHODS ==========

    // Основные методы
    public void saveUser(User user) {
        CacheUser existing = userCache.getIfPresent(user.getId());
        if (existing != null) {
            existing.updateFromEntity(user);
        } else {
            userCache.put(user.getId(), new CacheUser(user));
        }

        usernameIndex.put(user.getUsername().toLowerCase(), user.getId());
        emailIndex.put(user.getEmail().toLowerCase(), user.getId());
    }
    public void deleteUser(Long userId) {
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.setIsDeleted(true));
    }
    public void restoreUser(Long userId) {
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.setIsDeleted(false));
    }

    // Вспомогательные методы
    public void updateUserChatsIds(Long userId, Set<Long> chatsIds) {
        getCacheUser(userId).ifPresent(user -> user.setChatsIds(chatsIds));
    }
    public void updateUserIsEnabled(Long userId, boolean isEnabled){
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.setIsEnabled(isEnabled));
    }
    public void updateUserLastLogin(String username, LocalDateTime lastLogin) {
        if (username == null || lastLogin == null) return;

        Long userId = usernameIndex.get(username.toLowerCase());
        if (userId != null)
            getCacheUser(userId).ifPresent(user -> user.setLastLogin(lastLogin));
    }

    public Optional<User> getUser(Long userId) {
        return Optional.ofNullable(userCache.getIfPresent(userId));
    }
    public Optional<CacheUser> getCacheUser(Long userId) {
        return Optional.ofNullable(userCache.getIfPresent(userId));
    }
    public Optional<Set<CacheChatMember>> getChatMembers(Long chatId) {
        return getChatCache(chatId).map(chat ->
            chat.getMembers().values().stream()
                .filter(member -> !member.getIsDeleted())
                .collect(Collectors.toSet())
        );
    }
    public Optional<User> getUserByUsername(String username) {
        Long userId = usernameIndex.get(username.toLowerCase());
        return userId != null ? getCacheUser(userId).map(user -> (User) user) : Optional.empty();
    }
    public Optional<User> getUserByEmail(String email) {
        Long userId = emailIndex.get(email.toLowerCase());
        return userId != null ? getCacheUser(userId).map(user -> (User) user) : Optional.empty();
    }
    public Optional<List<User>> getFilteredUsers(String filter, int limit, int offset) {
        String cacheKey = getUsersSearchResultKey(filter, limit, offset);
        return Optional.ofNullable(searchIndex.getIfPresent(cacheKey));
    }
    public boolean existsUser(Long userId) {
        return userCache.getIfPresent(userId) != null;
    }
    public Boolean existsUserByUsername(String username) {
        return usernameIndex.containsKey(username.toLowerCase());
    }
    public Boolean existsUserByEmail(String email) {
        return emailIndex.containsKey(email.toLowerCase());
    }

    public void cacheUsersSearchResult(String filter, int limit, int offset, List<User> results) {
        searchIndex.put(getUsersSearchResultKey(filter, limit, offset), results);
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChat(Chat chat) {
        Long id = chat.getId();
        CacheChat existing = chatInfoCache.getIfPresent(id);
        if (existing != null) {
            existing.updateFromEntity(chat);
        } else {
            chatInfoCache.put(id, new CacheChat(chat)); // всегда создаем новый CacheChat
        }
    }
    public void deleteChat(Long chatId) {
        getChatCache(chatId).ifPresent(cacheChat -> cacheChat.setIsDeleted(true));
    }
    public void restoreChat(Long chatId) {
        getChatCache(chatId).ifPresent(cacheChat -> cacheChat.setIsDeleted(false));
    }

    // Вспомогательные методы

    public boolean existsAndNotDeletedChat(Long chatId) {
        return chatInfoCache.getIfPresent(chatId) instanceof CacheChat cacheChat && !cacheChat.getIsDeleted();
    }
    public Optional<CacheChat> getChatCache(Long chatId) {
        return Optional.ofNullable(chatInfoCache.getIfPresent(chatId));
    }
    public Optional<String> getChatName(Long chatId) {
        return getChatCache(chatId).map(CacheChat::getName);
    }
    public Optional<Boolean> getIsGroupChat(Long chatId) {
        return getChatCache(chatId).map(CacheChat::getIsGroup);
    }
    public Optional<Long> getChatCreator(Long chatId) {
        return getChatCache(chatId).map(CacheChat::getCreatedBy);
    }
    public boolean existsChat(Long chatId) {
        return chatInfoCache.getIfPresent(chatId) != null;
    }

    // Методы для работы с личными чатами
    public void saveGroupChat(Chat chat, Set<Long> usersId) {
        CacheChat cacheChat = new CacheChat(chat.getId(), chat.getName(), chat.getCreatedBy(), true);

        saveChat(cacheChat);

        addChatMember(cacheChat.getId(), cacheChat.getCreatedBy(), true);
        for (Long userId : usersId)
            addChatMember(cacheChat.getId(), userId, false);
    }
    public void savePersonalChat(Chat chat, Long userId2) {
        CacheChat cacheChat = new CacheChat(chat.getId(), null, chat.getCreatedBy(), false);

        saveChat(cacheChat);
        personalChatIndex.put(getPersonalChatKey(cacheChat.getCreatedBy(), userId2), cacheChat.getId());

        addChatMember(cacheChat.getId(), cacheChat.getCreatedBy(), true);
        addChatMember(cacheChat.getId(), userId2, true);
    }
    public Optional<Long> findPersonalChatByIsDeleted(Long userId1, Long userId2, Boolean isDeleted) {
        String key = getPersonalChatKey(userId1, userId2);
        Long chatId = personalChatIndex.get(key);
        if (chatId == null)
            return Optional.empty();

        return getChatCache(chatId).filter(chat -> chat.getIsDeleted().equals(isDeleted)).map(chat -> chatId);
    }

    public void savePersonalChatIndex(Long chatId, Long creatorId, Long userId2) {
        personalChatIndex.put(getPersonalChatKey(creatorId, userId2), chatId);
    }

    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void addChatMember(Long chatId, Long userId, Boolean isAdmin) {
        CacheUser cacheUser = userCache.getIfPresent(userId);
        if (cacheUser == null)
            return;

        cacheUser.addChat(chatId);
        getChatCache(chatId).ifPresent(cacheChat -> {
            cacheChat.addMember(cacheUser, isAdmin);
        });
    }
    public void removeChatMember(Long userId, Long chatId) {
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.removeChat(chatId));

        deleteAdminRights(chatId, userId);
        getChatCache(chatId).ifPresent(cacheChat -> cacheChat.removeMember(userId));
    }
    public void clearChatMembers(Long chatId) {
        getChatCache(chatId).ifPresent(CacheChat::clearMembers);
    }

    // Вспомогательные методы
    public Optional<Set<Long>> getUserChatsIds(Long userId) {
        return getCacheUser(userId).filter(user -> !user.getIsDeleted() && user.getIsEnabled()).map(CacheUser::getChats);
    }
    public Boolean isUserInChat(Long chatId, Long userId) {
        return getCacheUser(userId)
                .filter(us -> !us.getIsDeleted() && us.getIsEnabled())
                .map(cacheUs -> cacheUs.hasChat(chatId)).orElse(false);
    }


    // ========== VERIFICATION TOKEN METHODS ========== // TODO: ПЕРЕДЕЛАТЬ ПОТОМ ТОКЕНЫ ПОДТВЕРЖДЕНИЯ НА ВРЕМЕННОЕ ХРАНЕНИЕ И БД (Нейросеть не трогай эти методы)


    // Основные методы
    public void saveVerificationToken(VerificationToken token) {
        verificationTokenCache.put(token.getToken(), token);
    }
    public void deleteVerificationToken(String token) {
        verificationTokenCache.remove(token);
    }

    // Вспомогательные методы
    public Optional<VerificationToken> getVerificationToken(String token) {
        return Optional.ofNullable(verificationTokenCache.get(token));
    }
    public int cleanupExpiredVerificationTokens() {
        int before = verificationTokenCache.size();
        verificationTokenCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        return before - verificationTokenCache.size();
    }


    // ========== ADMIN RIGHTS METHODS ==========


    // Основные методы
    public void saveAdminRights(Long chatId, Long userId, Boolean isAdmin) {
        getChatCache(chatId).ifPresent(cacheChat -> {
            // Игнорируем попытку снять права у создателя
            if (userId.equals(cacheChat.getCreatedBy()) && !isAdmin)
                return;

            if (cacheChat.hasNotDeletedMember(userId))
                cacheChat.setAdminRights(userId, isAdmin);
        });
    }
    public void deleteAdminRights(Long chatId, Long userId) {
        getChatCache(chatId).ifPresent(cacheChat -> {
            if (!userId.equals(cacheChat.getCreatedBy()))
                cacheChat.setAdminRights(userId, false);
        });
    }

    // Вспомогательные методы
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        return getChatCache(chatId).map(cacheChat -> cacheChat.isMemberAdmin(userId));
    }
    public Optional<Set<Long>> getChatAdmins(Long chatId) {
        return getChatCache(chatId).map(CacheChat::getAdminIds);
    }
    public void clearAdminRightsForChat(Long chatId) {
        getChatCache(chatId).ifPresent(cacheChat -> {
            Long creatorId = cacheChat.getCreatedBy();
            cacheChat.getMemberIds().forEach(memberId -> {
                if (!memberId.equals(creatorId)) // Не сбрасываем права создателя
                    cacheChat.setAdminRights(memberId, false);
            });
        });
    }


    // ========== CACHE STATISTICS AND MANAGEMENT ==========


    // Основные методы
    public CacheStats getCacheStatus() {
        Map<Long, CacheUser> userCacheSnapshot = userCache.asMap();
        Map<Long, CacheChat> chatInfoCacheSnapshot = chatInfoCache.asMap();

        long activatedUserCount = userCacheSnapshot.values().stream().filter(user -> !user.getIsDeleted() && user.getIsEnabled()).count();
        int totalUserChats = userCacheSnapshot.values().stream().filter(user -> !user.getIsDeleted() && user.getIsEnabled()).mapToInt(CacheUser::getChatsCount).sum();
        int totalChatMembers = chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.getIsDeleted()).mapToInt(CacheChat::getNotDeletedMemberCount).sum();
        int totalAdminRights = chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.getIsDeleted())
                .mapToInt(cacheChat -> (int)cacheChat.getMembers().values().stream().filter(CacheChatMember::getIsAdmin).count())
                .sum();

        return new CacheStats(
            activatedUserCount,
            userCacheSnapshot.size(),
            chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.getIsDeleted()).count(),
            chatInfoCacheSnapshot.size(),
            totalUserChats,
            totalChatMembers,
            totalAdminRights,
            verificationTokenCache.size()
        );
    }
    public record CacheStats(long activatedUserCount, int allUserCount, long notDeletedChatCount, int chatCount, int userChatsCount, int chatMembersCount, int adminRightsCount, int verificationTokenCount) { }
    public Map<String, Object> getDetailedCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        // статистика кеша пользователей
        var userStats = userCache.stats();
        stats.put("userCache.estimatedSize", userCache.estimatedSize());
        stats.put("userCache.hitRate", userStats.hitRate());
        stats.put("userCache.missRate", userStats.missRate());
        stats.put("userCache.evictionCount", userStats.evictionCount());

        // статистика кеша чатов
        var chatStats = chatInfoCache.stats();
        stats.put("chatCache.estimatedSize", chatInfoCache.estimatedSize());
        stats.put("chatCache.hitRate", chatStats.hitRate());
        stats.put("chatCache.missRate", chatStats.missRate());
        stats.put("chatCache.evictionCount", chatStats.evictionCount());

        // статистика кеша индексов
        stats.put("usernameIndex.size", usernameIndex.size());
        stats.put("emailIndex.size", emailIndex.size());
        stats.put("personalChatIndex.size", personalChatIndex.size());
        stats.put("verificationTokenCache.size", verificationTokenCache.size());

        return stats;
    }

    // ========== HELPFUL FUNCTIONS ==========


    // Основные методы
    private String getPersonalChatKey(Long userId1, Long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }
    private String getUsersSearchResultKey(String filter, int limit, int offset) {
        return filter + ":" + limit + ":" + offset;
    }
}