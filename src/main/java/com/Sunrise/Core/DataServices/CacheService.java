package com.Sunrise.Core.DataServices;

import com.Sunrise.DTOs.Paginations.UserChatsPagination;
import com.Sunrise.DTOs.Paginations.ChatMembersPagination;
import com.Sunrise.Entities.Caches.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("NullableProblems")
@Slf4j
@Service
public class CacheService {

    // кэш пользователей
    private final Cache<Long, CacheUser> userCache = Caffeine.newBuilder()  // userId -> CacheUser (пользователи)
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS) // 1 h
            .recordStats()
            .build();

    private final Cache<String, Long> usernameIndex = Caffeine.newBuilder() // username -> userId (для регистрации)
            .maximumSize(150_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .softValues()  // при нехватке памяти delete-аем
            .recordStats()
            .build();
    private final Cache<String, Long> emailIndex = Caffeine.newBuilder() // email -> userId (для регистрации)
            .maximumSize(150_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .softValues()  // при нехватке памяти delete-аем
            .recordStats()
            .build();


    // кэш чатов
    private final Cache<Long, CacheChat> chatInfoCache = Caffeine.newBuilder() // chatId -> CacheChat (чаты)
            .maximumSize(50_000)
            .expireAfterAccess(12, TimeUnit.HOURS) // 12 h
            .build();

    private final Cache<String, Long> personalChatIndex = Caffeine.newBuilder() // "creatorId:userId" -> chatId (личные чаты)
            .maximumSize(100_000)
            .expireAfterAccess(12, TimeUnit.HOURS)
            .softValues() // при нехватке памяти delete-аем
            .recordStats()
            .build();

    private final Cache<String, UserChatsPagination> userChatsPaginationCache = Caffeine.newBuilder() // UserChatsPaginationKey -> UserChatsPagination
            .maximumSize(10_000)
            .expireAfterAccess(20, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final Map<Long, Set<String>> userPaginationKeys = new ConcurrentHashMap<>(); // userId -> Set<UserChatsPaginationKey>


    // контейнеры участников чата
    private final Cache<Long, CacheChatMembersContainer> chatMembersContainerCache = Caffeine.newBuilder()
            .maximumSize(200_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, ChatMembersPagination> chatMembersPaginationCache = Caffeine.newBuilder() // "chatId:offset:limit" -> paginationId
            .maximumSize(50_000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final Map<Long, Set<String>> chatMembersPaginationKeys = new ConcurrentHashMap<>();


    // кеш токенов подтверждения
    private final Cache<String, CacheVerificationToken> verificationTokenCache = Caffeine.newBuilder() // token -> CacheVerificationToken (токены подтверждения)
            .maximumSize(50_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(CacheUser user) {
        Optional<CacheUser> existing = getCacheUser(user.getId());
        if (existing.isPresent()) {
            existing.get().updateFromCache(user);
        } else {
            userCache.put(user.getId(), user);
        }

        usernameIndex.put(user.getUsername().toLowerCase(), user.getId());
        emailIndex.put(user.getEmail().toLowerCase(), user.getId());
    }
    public void saveNewUser(CacheUser user) {
        saveUser(user);
    }
    public void deleteUser(long userId) {
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.setDeleted(true));

        // инвалидируем пагинацию
        invalidateUserChatsPagination(userId);
    }
    public void restoreUser(long userId) {
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.setDeleted(false));

        // инвалидируем пагинацию
        invalidateUserChatsPagination(userId);
    }


    // Вспомогательные методы
    public void updateUserIsEnabled(long userId, boolean isEnabled){
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.setEnabled(isEnabled));
    }
    public void updateUserLastLogin(String username, LocalDateTime lastLogin) {
        getUserByUsername(username).ifPresent(user -> user.setLastLogin(lastLogin));
    }
    public void updateUserProfile(long userId, String username, String name) {
        getCacheUser(userId).ifPresent(user -> {
            // Обновляем username в индексе
            String oldUsername = user.getUsername();
            if (!oldUsername.equals(username)) {
                usernameIndex.invalidate(oldUsername.toLowerCase());
                usernameIndex.put(username.toLowerCase(), userId);
            }

            // Обновляем данные пользователя
            user.setUsername(username);
            user.setName(name);
        });
    }


    public Optional<CacheUser> getCacheUser(long userId) {
        return Optional.ofNullable(userCache.getIfPresent(userId));
    }
    public Optional<CacheUser> getUserByUsername(String username) {
        String key = username.toLowerCase();
        Long userId = usernameIndex.getIfPresent(key);
        if (userId == null) return Optional.empty();

        Optional<CacheUser> user = getCacheUser(userId);
        if (user.isEmpty()) {
            usernameIndex.invalidate(key);
        }
        return user;
    }
    public boolean existsUser(long userId) {
        return getCacheUser(userId).isPresent();
    }
    public boolean existsUserByUsername(String username) {
        return usernameIndex.getIfPresent(username.toLowerCase()) != null;
    }
    public boolean existsUserByEmail(String email) {
        return emailIndex.getIfPresent(email.toLowerCase()) != null;
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChat(CacheChat newChat) {
        CacheChat oldChat = chatInfoCache.getIfPresent(newChat.getId());
        if (oldChat != null) {
            oldChat.updateFromCache(newChat);
        } else {
            chatInfoCache.put(newChat.getId(), newChat);
            if(!newChat.isGroup()) {
                savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
            }
        }
    }
    public void updateChatCreator(long chatId, long newCreatorId) {
        getCacheChat(chatId)
                .ifPresent(chat -> chat.setCreatedBy(newCreatorId));

        getChatMembersContainer(chatId)
                .ifPresent(container -> container.updateChatCreator(newCreatorId));
    }
    public void deleteChat(long chatId, List<Long> membersIds) {
        getCacheChat(chatId).ifPresent(CacheChat::delete);

        // инвалидация пагинации
        membersIds.forEach(this::invalidateUserChatsPagination);
        log.debug("[⚡] Invalidated pagination cache for {} users | cacheService/deleteChat", membersIds.size());
    }
    public void restoreChat(long chatId, List<Long> membersIds) {
        getCacheChat(chatId).ifPresent(CacheChat::restore);

        // инвалидация пагинации
        membersIds.forEach(this::invalidateUserChatsPagination);
        log.debug("[⚡] Invalidated pagination cache for {} users | cacheService/restoreChat", membersIds.size());
    }


    // Вспомогательные методы
    public boolean isActiveChat(long chatId) {
        return getActiveChat(chatId).isPresent();
    }

    public Optional<CacheChat> getCacheChat(long chatId) {
        return Optional.ofNullable(chatInfoCache.getIfPresent(chatId));
    }
    public Optional<CacheChat> getActiveChat(long chatId) {
        return getCacheChat(chatId).filter(CacheChat::isActive);
    }
    public Optional<CacheChat> getPersonalChat(long userId1, long userId2) {
        String key = getPersonalChatKey(userId1, userId2);
        Long chatId = personalChatIndex.getIfPresent(key);
        if (chatId == null) return Optional.empty();

        Optional<CacheChat> chat = getCacheChat(chatId);
        if (chat.isEmpty()) personalChatIndex.invalidate(key);
        return chat;
    }
    public Optional<Boolean> isActiveGroupChat(long chatId) {
        return getActiveChat(chatId).map(CacheChat::isGroup);
    }


    // Методы для сохранения индекса личного чата TODO: НАДО ПОДУМАТЬ ЧТО СДЕЛАТЬ
    private String getPersonalChatKey(long userId1, long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }
    public void savePersonalChatIndex(long chatId, long creatorId, long opponentId) {
        personalChatIndex.put(getPersonalChatKey(creatorId, opponentId), chatId);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    private CacheChatMembersContainer getOrCreateChatMembersContainer(CacheChat chat) {
        return chatMembersContainerCache.get(chat.getId(), key -> new CacheChatMembersContainer(chat));
    }
    private Optional<CacheChatMembersContainer> getChatMembersContainer(long chatId) {
        return Optional.ofNullable(chatMembersContainerCache.getIfPresent(chatId));
    }

    public void addChatMembers(CacheChat chat, List<CacheChatMember> members) {
        // Обновляем контейнер
        getOrCreateChatMembersContainer(chat).addMembers(members);

        // Обновляем кэш пользователя
        members.forEach(m -> {
            getCacheUser(m.getUserId()).ifPresent(user -> user.addChat(chat.getId()));
        });
    }
    public void addNewChatMembers(CacheChat chat, List<CacheChatMember> members) {
        // Обновляем контейнер
        getOrCreateChatMembersContainer(chat).addNewMembers(members);

        // Обновляем кэш пользователя
        members.forEach(m -> {
            getCacheUser(m.getUserId()).ifPresent(user -> user.addChat(chat.getId()));
            invalidateChatMembersPagination(chat.getId());
            invalidateUserChatsPagination(m.getUserId());
        });
        log.debug("[⚡] Invalidated pagination cache for {} users | cacheService/addNewChatMembers", members.size());
    }

    public void addChatMember(CacheChat chat, CacheChatMember chatMember) {
        // Обновляем контейнер НОВЫМ пользователем (кеш созданного только что пользователя)
        getOrCreateChatMembersContainer(chat).addMember(chatMember);

        // Обновляем кэш пользователя
        getCacheUser(chatMember.getUserId()).ifPresent(user -> user.addChat(chatMember.getChatId()));
    }
    public void addNewChatMember(CacheChat chat, CacheChatMember chatMember) {
        // Обновляем контейнер СТАРЫМ пользователем (просто кеш существующего пользователя)
        getOrCreateChatMembersContainer(chat).addNewMember(chatMember);

        // Обновляем кэш пользователя
        getCacheUser(chatMember.getUserId()).ifPresent(user -> user.addChat(chatMember.getChatId()));

        // инвалидация пагинации
        invalidateChatMembersPagination(chat.getId());
        invalidateUserChatsPagination(chatMember.getUserId());
        log.debug("[⚡] Invalidated pagination cache for user {} | cacheService/addNewChatMember", chatMember.getUserId());
    }

    public void removeChatMember(long userId, long chatId) {
        // Обновляем контейнер
        getChatMembersContainer(chatId).ifPresent(c -> c.markMemberAsDeleted(userId));

        // Обновляем кэш пользователя
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.removeChat(chatId));

        // инвалидация пагинации
        invalidateChatMembersPagination(chatId);
        invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | cacheService/removeChatMember", userId);
    }
    public void restoreChatMember(long userId, long chatId, boolean isAdmin) {
        // Обновляем контейнер
        getChatMembersContainer(chatId).ifPresent(c -> c.restoreMember(userId, isAdmin));

        // Обновляем кэш пользователя
        getCacheUser(userId).ifPresent(user -> user.addChat(chatId));

        // инвалидация пагинации
        invalidateChatMembersPagination(chatId);
        invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | cacheService/restoreChatMember", userId);
    }


    // Вспомогательные методы
    public Optional<CacheChatMember> getChatMember(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.getMember(userId));
    }
    public Optional<Boolean> hasActiveChatMember(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.hasMemberAndGetIsActive(userId));
    }
    public Optional<SimpleEntry<Set<Long>, Boolean>> getUserChatsIds(long userId) {
        return getCacheUser(userId).map(CacheUser::getChatsIdsAndIsFullyLoaded);
    }


    // ========== PAGINATION METHODS ==========


    // пагинация для выдачи чатов пользователей

    private String getUserChatsPaginationKey(long userId, Long cursor, int limit) {
        return userId + ":" + (cursor == null ? "" : cursor) + ":" + limit;
    }
    public void saveUserChatsPagination(UserChatsPagination pagination) {
        String key = getUserChatsPaginationKey(pagination.getUserId(), pagination.getCursor(), pagination.getLimit());
        userChatsPaginationCache.put(key, pagination);
        userPaginationKeys.computeIfAbsent(pagination.getUserId(), k -> ConcurrentHashMap.newKeySet()).add(key);
    }
    public Optional<UserChatsPagination> findUserChatsPagination(long userId, Long cursor, int limit) {
        String key = getUserChatsPaginationKey(userId, cursor, limit);
        return Optional.ofNullable(userChatsPaginationCache.getIfPresent(key));
    }
    public void invalidateUserChatsPagination(long userId) {
        Set<String> keys = userPaginationKeys.remove(userId);
        if (keys != null)
            keys.forEach(userChatsPaginationCache::invalidate);
    }


    // пагинация для выдачи участников чата
    private String getChatMembersPaginationKey(long chatId, Long cursor, int limit) {
        return chatId + ":" + (cursor == null ? "" : cursor) + ":" + limit;
    }
    public void saveChatMembersPagination(ChatMembersPagination pagination) {
        String key = getChatMembersPaginationKey(pagination.getChatId(), pagination.getCursor(), pagination.getLimit());
        chatMembersPaginationCache.put(key, pagination);
        chatMembersPaginationKeys.computeIfAbsent(pagination.getChatId(), k -> ConcurrentHashMap.newKeySet()).add(key);
    }
    public Optional<ChatMembersPagination> findChatMembersPagination(long chatId, Long cursor, int limit) {
        String key = getChatMembersPaginationKey(chatId, cursor, limit);
        return Optional.ofNullable(chatMembersPaginationCache.getIfPresent(key));
    }
    public void invalidateChatMembersPagination(long chatId) {
        Set<String> keys = chatMembersPaginationKeys.remove(chatId);
        if (keys != null)
            keys.forEach(chatMembersPaginationCache::invalidate);
    }


    // ========== MESSAGES METHODS ==========



    // ========== VERIFICATION TOKEN METHODS ==========


    public void saveVerificationToken(CacheVerificationToken cacheVerificationToken) {
        verificationTokenCache.put(cacheVerificationToken.getToken(), cacheVerificationToken);
    }
    public void deleteVerificationToken(String token) {
        verificationTokenCache.invalidate(token);
    }
    public Optional<CacheVerificationToken> getVerificationToken(String token) {
        return Optional.ofNullable(verificationTokenCache.getIfPresent(token));
    }



    // ========== ADMIN RIGHTS METHODS ==========


    // Основные методы
    public void saveOrUpdateAdminRights(long chatId, long userId, boolean isAdmin) {
        getChatMembersContainer(chatId)
            .ifPresent(cont -> cont.updateAdminRights(userId, isAdmin));
    }


    // Вспомогательные методы
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.isAdmin(userId));
    }
    public Optional<List<CacheChatMember>> getChatAdmins(long chatId) {
        return getChatMembersContainer(chatId).map(CacheChatMembersContainer::getChatAdmins);
    }
    public void updateLastReadByUser(long chatId, long userId, long messageId) {
        getCacheChat(chatId).ifPresent(chat -> chat.updateLastReadByUser(messageId, userId));
    }


    // ========== CACHE STATISTICS AND MANAGEMENT ==========


    // Основные методы
    public record CacheStats(
            long activatedUserCount,
            int allUserCount,
            long notDeletedChatCount,
            int chatCount,
            int userChatsCount,
            int chatMembersCount,
            int adminRightsCount,
            int verificationTokenCount,
            int deletedMembersCount
    ) {}

    public CacheStats getCacheStatus() {
        Map<Long, CacheUser> userCacheSnapshot = userCache.asMap();
        Map<Long, CacheChat> chatInfoCacheSnapshot = chatInfoCache.asMap();
        Map<Long, CacheChatMembersContainer> containersSnapshot = chatMembersContainerCache.asMap();

        long activatedUserCount = userCacheSnapshot.values().stream()
                .filter(user -> !user.isDeleted() && user.isEnabled())
                .count();

        int totalUserChats = userCacheSnapshot.values().stream()
                .filter(user -> !user.isDeleted() && user.isEnabled())
                .mapToInt(CacheUser::getChatsCount)
                .sum();

        int totalChatMembers = containersSnapshot.values().stream()
                .mapToInt(container -> container.getMembers().size())
                .sum();

        int totalAdminRights = containersSnapshot.values().stream()
                .mapToInt(container -> container.getAdminIds().size())
                .sum();

        int totalDeletedMembers = containersSnapshot.values().stream()
                .mapToInt(container -> container.getDeletedMemberIds().size())
                .sum();

        return new CacheStats(
                activatedUserCount,
                userCacheSnapshot.size(),
                chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.isDeleted()).count(),
                chatInfoCacheSnapshot.size(),
                totalUserChats,
                totalChatMembers,
                totalAdminRights,
                (int)verificationTokenCache.estimatedSize(),
                totalDeletedMembers
        );
    }
    public void printCacheStats() {
        CacheService.CacheStats stats = getCacheStatus();
        log.info("📊 Cache Statistics:");
        log.info("   ├─ Active Users: {}", stats.allUserCount());
        log.info("   ├─ Activated Users: {}", stats.activatedUserCount());
        log.info("   ├─ Users: {}", stats.allUserCount());
        log.info("   ├─ Active Chats: {}", stats.chatCount());
        log.info("   ├─ Active Sessions: {}", stats.allUserCount());
        log.info("   ├─ Verification Tokens: {}", stats.verificationTokenCount());
        log.info("   ├─ User-Chat Relations: {}", stats.userChatsCount());
        log.info("   ├─ Chat Members: {}", stats.chatMembersCount());
        log.info("   └─ Admin Rights: {}", stats.adminRightsCount());
    }

    public Map<String, Object> getDetailedCacheStatus() {
        Map<String, Object> stats = new HashMap<>();

        // статистика кеша пользователей
        var userStats = userCache.stats();
        stats.put("userCache.estimatedSize", userCache.estimatedSize());
        stats.put("userCache.hitRate", userStats.hitRate());
        stats.put("userCache.missRate", userStats.missRate());
        stats.put("userCache.evictionCount", userStats.evictionCount());

        var userChatsPaginationStats = userChatsPaginationCache.stats();
        stats.put("userChatsPaginationCache.estimatedSize", userChatsPaginationCache.estimatedSize());
        stats.put("userChatsPaginationCache.hitRate", userChatsPaginationStats.hitRate());
        stats.put("userChatsPaginationCache.missRate", userChatsPaginationStats.missRate());
        stats.put("userChatsPaginationCache.evictionCount", userChatsPaginationStats.evictionCount());

        // статистика кеша чатов
        var chatStats = chatInfoCache.stats();
        stats.put("chatCache.estimatedSize", chatInfoCache.estimatedSize());
        stats.put("chatCache.hitRate", chatStats.hitRate());
        stats.put("chatCache.missRate", chatStats.missRate());
        stats.put("chatCache.evictionCount", chatStats.evictionCount());

        var containerStats = chatMembersContainerCache.stats();
        stats.put("chatMembersContainerCache.estimatedSize", chatMembersContainerCache.estimatedSize());
        stats.put("chatMembersContainerCache.hitRate", containerStats.hitRate());
        stats.put("chatMembersContainerCache.missRate", containerStats.missRate());
        stats.put("chatMembersContainerCache.evictionCount", containerStats.evictionCount());

        var tokenStats = verificationTokenCache.stats();
        stats.put("tokenCache.estimatedSize", verificationTokenCache.estimatedSize());
        stats.put("tokenCache.hitRate", tokenStats.hitRate());
        stats.put("tokenCache.missRate", tokenStats.missRate());
        stats.put("tokenCache.evictionCount", tokenStats.evictionCount());

        // статистика кеша индексов
        stats.put("usernameIndex.size", usernameIndex.estimatedSize());
        stats.put("emailIndex.size", emailIndex.estimatedSize());
        stats.put("personalChatIndex.size", personalChatIndex.estimatedSize());

        return stats;
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 10_000) // 1000 * 60 * 60
    public void logDetailedCacheStatus() {

        var cacheStats = getDetailedCacheStatus();

        log.info("---------------------------");

        printCacheStats(); // Выводим основную статистику

        log.info("📊 Cache Statistics Report");
        log.info("   ├─ User Cache: size={}, hitRate={}, missRate={}, evictions={}",
                cacheStats.get("userCache.estimatedSize"),
                (Double)cacheStats.get("userCache.hitRate") * 100,
                (Double)cacheStats.get("userCache.missRate") * 100,
                cacheStats.get("userCache.evictionCount"));

        log.info("   ├─ Chat Cache: size={}, hitRate={}, missRate={}, evictions={}",
                cacheStats.get("chatCache.estimatedSize"),
                (Double)cacheStats.get("chatCache.hitRate") * 100,
                (Double)cacheStats.get("chatCache.missRate") * 100,
                cacheStats.get("chatCache.evictionCount"));

        log.info("   ├─ Chat Member Cache: size={}, hitRate={}%, missRate={}%, evictions={}",
                cacheStats.get("chatMembersContainerCache.estimatedSize"),
                Math.round((Double)cacheStats.get("chatMembersContainerCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("chatMembersContainerCache.missRate") * 100),
                cacheStats.get("chatMembersContainerCache.evictionCount"));

        log.info("   ├─ Token Cache: size={}, hitRate={}, missRate={}, evictions={}",
                cacheStats.get("tokenCache.estimatedSize"),
                (Double)cacheStats.get("tokenCache.hitRate") * 100,
                (Double)cacheStats.get("tokenCache.missRate") * 100,
                cacheStats.get("tokenCache.evictionCount"));

        log.info("   ├─ Indexes: username={}, email={}, personalChats={}",
                cacheStats.get("usernameIndex.size"),
                cacheStats.get("emailIndex.size"),
                cacheStats.get("personalChatIndex.size"));

        log.info("---------------------------");
    }

    @Scheduled(fixedDelay = 3_600_000, initialDelay = 10_000) // 1000 * 60 * 60
    public void cleanupPaginationKeys() {
        int beforeUserKeys = userPaginationKeys.values().stream().mapToInt(Set::size).sum();
        int beforeChatKeys = chatMembersPaginationKeys.values().stream().mapToInt(Set::size).sum();

        userPaginationKeys.entrySet().removeIf(entry -> {
            // Удаляем конкретные ключи, которые уже вытеснены из кеша
            entry.getValue().removeIf(key ->
                    userChatsPaginationCache.getIfPresent(key) == null
            );
            return entry.getValue().isEmpty();
        });
        chatMembersPaginationKeys.entrySet().removeIf(entry -> {
            // Удаляем конкретные ключи, которые уже вытеснены из кеша
            entry.getValue().removeIf(key ->
                    chatMembersPaginationCache.getIfPresent(key) == null
            );
            return entry.getValue().isEmpty();
        });

        int afterUserKeys = userPaginationKeys.values().stream().mapToInt(Set::size).sum();
        int afterChatKeys = chatMembersPaginationKeys.values().stream().mapToInt(Set::size).sum();

        if (beforeUserKeys != afterUserKeys || beforeChatKeys != afterChatKeys) {
            log.debug("[🧹] Pagination cleanup: user keys {}→{}, chat keys {}→{}", beforeUserKeys, afterUserKeys, beforeChatKeys, afterChatKeys);
        }
    }
}