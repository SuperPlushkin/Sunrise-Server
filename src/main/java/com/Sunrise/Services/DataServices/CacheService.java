package com.Sunrise.Services.DataServices;

import com.Sunrise.Entities.Cache.ChatMembersContainer;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Entities.DB.User;
import com.Sunrise.Entities.DB.Chat;
import com.Sunrise.Entities.DB.VerificationToken;
import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import com.Sunrise.Entities.Cache.CacheUser;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Builder;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

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

    private final Cache<Long, UsersPagination> usersPaginationCache = Caffeine.newBuilder() // для getFilteredUsers (Это можно будет заменить потом на ElasticSearch)
            .maximumSize(5_000) // меньше чем для чатов, т.к. поиск пользователей реже
            .expireAfterWrite(2, TimeUnit.MINUTES) // более короткий TTL для поиска
            .recordStats()
            .build();

    private final Map<String, Long> usersPaginationIndex = new ConcurrentHashMap<>(); // "filter:offset:limit" -> paginationId


    // кэш чатов
    private final Cache<Long, CacheChat> chatInfoCache = Caffeine.newBuilder() // chatId -> CacheChat (чаты)
            .maximumSize(50_000)
            .expireAfterAccess(12, TimeUnit.HOURS) // 12 h
            .build();

    private final Map<String, Long> personalChatIndex = new ConcurrentHashMap<>(1000); // "creatorId:userId" -> chatId (личные чаты)

    private final Cache<Long, UserChatsPagination> userChatsPaginationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final Map<String, Long> userChatsPaginationIndex = new ConcurrentHashMap<>(); // "userId:offset:limit" -> paginationId


    // контейнеры участников чата
    private final Cache<Long, ChatMembersContainer> chatMembersContainerCache = Caffeine.newBuilder()
            .maximumSize(200_000)
            .expireAfterWrite(1, TimeUnit.HOURS) // Всё истекает вместе
            .recordStats()
            .build();

    private final Cache<Long, ChatMembersPagination> chatMembersPaginationCache = Caffeine.newBuilder()
            .maximumSize(50_000) // Меньше чем для чатов, так как страниц может быть много
            .expireAfterWrite(2, TimeUnit.MINUTES) // Короткий TTL для актуальности
            .recordStats()
            .build();

    private final Map<String, Long> chatMembersPaginationIndex = new ConcurrentHashMap<>(); // "chatId:offset:limit:sortBy" -> paginationId


    // кеш токенов подтверждения
    private final Cache<String, VerificationToken> verificationTokenCache = Caffeine.newBuilder() // token -> VerificationToken (токены подтверждения)
            .maximumSize(50_000)
            .expireAfterWrite(1, TimeUnit.HOURS) // 1 h
            .build();


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(User user) {
        Optional<CacheUser> existing = getCacheUser(user.getId());
        if (existing.isPresent()) {
            existing.get().updateFromEntity(user);
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

        getUserByUsername(username).ifPresent(user -> user.setLastLogin(lastLogin));
    }

    public Optional<CacheUser> getCacheUser(Long userId) {
        return Optional.ofNullable(userCache.getIfPresent(userId));
    }
    public Optional<CacheUser> getUserByUsername(String username) {
        Long userId = usernameIndex.get(username.toLowerCase());
        return userId != null ? getCacheUser(userId) : Optional.empty();
    }
    public Optional<CacheUser> getUserByEmail(String email) {
        Long userId = emailIndex.get(email.toLowerCase());
        return userId != null ? getCacheUser(userId) : Optional.empty();
    }
    public boolean existsUser(Long userId) {
        return getCacheUser(userId).isPresent();
    }
    public Boolean existsUserByUsername(String username) {
        return usernameIndex.containsKey(username.toLowerCase());
    }
    public Boolean existsUserByEmail(String email) {
        return emailIndex.containsKey(email.toLowerCase());
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public CacheChat saveExistingChat(Chat chat) {
        CacheChat cacheChat = chatInfoCache.getIfPresent(chat.getId());
        if (cacheChat != null) {
            cacheChat.updateFromEntity(chat);
        } else {
            cacheChat = new CacheChat(chat);
            chatInfoCache.put(cacheChat.getId(), cacheChat); // всегда создаем новый CacheChat
        }
        return cacheChat;
    }
    public void saveNewGroupChat(Chat chat, List<ChatMember> members) {
        saveExistingChat(chat);

        // Сохраняем участников как Map
        addChatMembers(chat, members);

        // Обновляем кэши пользователей
        members.forEach(member ->
            getCacheUser(member.getUserId()).ifPresent(user -> user.addChat(chat.getId()))
        );
    }
    public void saveNewPersonalChat(Chat chat, ChatMember creator, ChatMember member) {
        saveExistingChat(chat);

        if (!chat.getIsGroup()) {
            savePersonalChatIndex(creator.getUserId(), member.getUserId(), chat.getId());
        }

        // Сохраняем участников как Map
        addChatMembers(chat, List.of(creator, member));

        // Обновляем кэши пользователей
        getCacheUser(creator.getUserId()).ifPresent(user -> user.addChat(chat.getId()));
        getCacheUser(member.getUserId()).ifPresent(user -> user.addChat(chat.getId()));
    }

    public void deleteChat(Long chatId) {
        getChatCache(chatId).ifPresent(cacheChat -> cacheChat.setIsDeleted(true));
        chatMembersContainerCache.invalidate(chatId);
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
    public Optional<CacheChat> getPersonalChat(Long userId1, Long userId2) {
        String key = getPersonalChatKey(userId1, userId2);
        Long chatId = personalChatIndex.get(key);
        if (chatId == null)
            return Optional.empty();

        return getChatCache(chatId);
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


    // Методы для сохранения индекса личного чата TODO: НАДО ПОДУМАТЬ ЧТО СДЕЛАТЬ
    public void savePersonalChatIndex(Long chatId, Long creatorId, Long userId2) {
        personalChatIndex.put(getPersonalChatKey(creatorId, userId2), chatId);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    private ChatMembersContainer getOrCreateChatMembersContainer(Chat chat) {
        return chatMembersContainerCache.get(chat.getId(), key -> new ChatMembersContainer(chat));
    }
    public Optional<ChatMembersContainer> getChatMembersContainer(Long chatId) {
        return Optional.ofNullable(chatMembersContainerCache.getIfPresent(chatId));
    }

    public void addChatMembers(Chat chat, List<ChatMember> members) {
        // Обновляем контейнер
        List<CacheChatMember> cacheMembers = members.stream().map(CacheChatMember::new).toList();
        getOrCreateChatMembersContainer(chat).addMembers(cacheMembers);

        // Обновляем кэш пользователя
        members.forEach(member -> getCacheUser(member.getUserId()).ifPresent(user -> user.addChat(chat.getId())));
    }
    public void addNewChatMembers(Chat chat, List<ChatMember> members) {
        // Обновляем контейнер
        List<CacheChatMember> cacheMembers = members.stream().map(CacheChatMember::new).toList();
        getOrCreateChatMembersContainer(chat).addMembers(cacheMembers);

        // Обновляем кэш пользователя
        members.forEach(member -> getCacheUser(member.getUserId()).ifPresent(user -> user.addChat(chat.getId())));
    }

    public void addChatMember(Chat chat, ChatMember chatMember) {
        // Обновляем контейнер НОВЫМ пользователем (кеш созданного только что пользователя)
        getOrCreateChatMembersContainer(chat).addMember(new CacheChatMember(chatMember));

        // Обновляем кэш пользователя
        getCacheUser(chatMember.getUserId()).ifPresent(user -> user.addChat(chatMember.getChatId()));
    }
    public void addNewChatMember(Chat chat, ChatMember chatMember) {
        // Обновляем контейнер СТАРЫМ пользователем (просто кеш существующего пользователя)
        getOrCreateChatMembersContainer(chat).addNewMember(new CacheChatMember(chatMember));

        // Обновляем кэш пользователя
        getCacheUser(chatMember.getUserId()).ifPresent(user -> user.addChat(chatMember.getChatId()));
    }

    public void removeChatMember(Long userId, Long chatId) {
        // Обновляем контейнер
        getChatMembersContainer(chatId).ifPresent(c -> c.markMemberAsDeleted(userId));

        // Обновляем кэш пользователя
        getCacheUser(userId).ifPresent(cacheUser -> cacheUser.removeChat(chatId));
    }
    public void restoreChatMember(Long userId, Long chatId, Boolean isAdmin) {
        // Обновляем контейнер
        getChatMembersContainer(chatId).ifPresent(c -> c.restoreMember(userId, isAdmin));

        // Обновляем кэш пользователя
        getCacheUser(userId).ifPresent(user -> user.addChat(chatId));
    }


    // Вспомогательные методы
    public Optional<List<CacheChatMember>> getChatMembers(Long chatId) {
        return getChatMembersContainer(chatId).map(c -> new ArrayList<>(c.getMembers().values()));
    }
    public Optional<List<CacheChatMember>> getActiveChatMembers(Long chatId) {
        return getChatMembersContainer(chatId).map(ChatMembersContainer::getActiveMembers);
    }
    public Optional<List<CacheChatMember>> getChatMembersPage(Long chatId, int offset, int limit) {
//        return getChatMembersContainer(chatId).map(container -> container.getMembersPage(offset, limit));
        return null;
    }
    public Optional<CacheChatMember> getChatMember(Long chatId, Long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.getMember(userId));
    }
    public Optional<Boolean> hasChatMember(Long chatId, Long userId) {
        return getChatMembersContainer(chatId).map(c -> c.hasMember(userId));
    }


    public Optional<Set<Long>> getUserChatsIds(Long userId) {
        return getCacheUser(userId).map(CacheUser::getChats);
    }
    public Optional<Boolean> isUserInChat(Long chatId, Long userId) {
        return getChatMembersContainer(chatId)
                .map(cont -> cont.hasMember(userId))
                .or(() -> getCacheUser(userId).map(user -> user.hasChat(chatId)));
    }

    // cache методы

    public void invalidateAfterMemberAdded(Long chatId, Long userId) {
        invalidateChatMembersPagination(chatId);
        invalidateUserChatsPagination(userId);
    }
    public void invalidateAfterMemberRemoved(Long chatId, Long userId) {
        invalidateChatMembersPagination(chatId);
        invalidateUserChatsPagination(userId);
    }

    public void invalidateAfterChatAdded(List<Long> membersIds) {
        membersIds.forEach(this::invalidateUserChatsPagination);
    }
    public void invalidateAfterChatDeleted(List<Long> membersIds) {
        membersIds.forEach(this::invalidateUserChatsPagination);
    }
    public void invalidateAfterChatRestored(List<Long> membersIds) {
        membersIds.forEach(this::invalidateUserChatsPagination);
    }


    // ========== PAGINATION METHODS ==========

    @Value
    @Builder
    public static class UserChatsPagination {
        long id;
        long userId;
        int offset;
        int limit;
        List<Long> chatIds;
        LocalDateTime createdAt;
        boolean hasMore;
        int totalCount;
    }

    public void saveUserChatsPagination(UserChatsPagination pagination) {
        userChatsPaginationCache.put(pagination.getId(), pagination);

        String key = getUserChatsPaginationKey(pagination.getUserId(), pagination.getOffset(), pagination.getLimit());
        userChatsPaginationIndex.put(key, pagination.getId());
    }
    public Optional<UserChatsPagination> findUserChatsPagination(long userId, int offset, int limit) {
        String key = getUserChatsPaginationKey(userId, offset, limit);
        Long paginationId = userChatsPaginationIndex.get(key);
        if (paginationId == null)
            return Optional.empty();

        UserChatsPagination pagination = userChatsPaginationCache.getIfPresent(paginationId);
        if (pagination == null) {
            userChatsPaginationIndex.remove(key); // Очищаем битую ссылку
            return Optional.empty();
        }

        return Optional.of(pagination);
    }
    public void invalidateUserChatsPagination(long userId) {
        userChatsPaginationIndex.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(userId + ":")) {
                userChatsPaginationCache.invalidate(entry.getValue());
                return true;
            }
            return false;
        });
    }


    @Value
    @Builder
    public static class UsersPagination {
        long id;
        String filter;           // поисковый фильтр (может быть пустым)
        int offset;
        int limit;
        List<Long> userIds;      // храним только ID пользователей
        LocalDateTime createdAt;
        boolean hasMore;
        int totalCount;
    }

    public void saveUsersPagination(UsersPagination pagination) {
        usersPaginationCache.put(pagination.getId(), pagination);

        String key = getUsersPaginationKey(pagination.getFilter(), pagination.getOffset(), pagination.getLimit());
        usersPaginationIndex.put(key, pagination.getId());
    }
    public Optional<UsersPagination> findUsersPagination(String filter, int offset, int limit) {
        String key = getUsersPaginationKey(filter, offset, limit);
        Long paginationId = usersPaginationIndex.get(key);
        if (paginationId == null)
            return Optional.empty();

        UsersPagination pagination = usersPaginationCache.getIfPresent(paginationId);
        if (pagination == null) {
            usersPaginationIndex.remove(key); // Очищаем битую ссылку
            return Optional.empty();
        }

        return Optional.of(pagination);
    }
    public void invalidateUsersPagination() {
        // При изменении пользователей инвалидируем весь кеш поиска
        usersPaginationCache.invalidateAll();
        usersPaginationIndex.clear();
    }


    @Value
    @Builder
    public static class ChatMembersPagination {
        long id;
        long chatId;
        int offset;
        int limit;
        List<Long> memberUserIds; // храним только ID пользователей
        LocalDateTime createdAt;
        boolean hasMore;
        int totalCount;
    }

    public void saveChatMembersPagination(ChatMembersPagination pagination) {
        chatMembersPaginationCache.put(pagination.getId(), pagination);

        String key = getChatMembersPaginationKey(
            pagination.getChatId(),
            pagination.getOffset(),
            pagination.getLimit()
        );
        chatMembersPaginationIndex.put(key, pagination.getId());
    }
    public Optional<ChatMembersPagination> findChatMembersPagination(long chatId, int offset, int limit) {
        String key = getChatMembersPaginationKey(chatId, offset, limit);
        Long paginationId = chatMembersPaginationIndex.get(key);

        if (paginationId == null)
            return Optional.empty();

        ChatMembersPagination pagination = chatMembersPaginationCache.getIfPresent(paginationId);
        if (pagination == null)
            chatMembersPaginationIndex.remove(key); // Очищаем битую ссылку

        return Optional.ofNullable(pagination);
    }
    public void invalidateChatMembersPagination(long chatId) {
        chatMembersPaginationIndex.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(chatId + ":")) {
                chatMembersPaginationCache.invalidate(entry.getValue());
                return true;
            }
            return false;
        });
    }


    // ========== VERIFICATION TOKEN METHODS ========== // TODO: ПЕРЕДЕЛАТЬ ПОТОМ ТОКЕНЫ ПОДТВЕРЖДЕНИЯ НА ВРЕМЕННОЕ ХРАНЕНИЕ И БД (Нейросеть не трогай эти методы)


    // Основные методы
    public void saveVerificationToken(VerificationToken token) {
        verificationTokenCache.put(token.getToken(), token);
    }
    public void deleteVerificationToken(String token) {
        verificationTokenCache.invalidate(token);
    }


    // Вспомогательные методы
    public Optional<VerificationToken> getVerificationToken(String token) {
        return Optional.ofNullable(verificationTokenCache.getIfPresent(token));
    }


    // ========== ADMIN RIGHTS METHODS ==========


    // Основные методы
    public void saveAdminRights(Long chatId, Long userId, Boolean isAdmin) {
        getChatMembersContainer(chatId)
            .ifPresent(cont -> cont.updateAdminRights(userId, isAdmin));
    }
    public void updateChatCreator(Long chatId, Long newCreatorId) {
        getChatCache(chatId)
            .ifPresent(cht -> cht.setCreatedBy(newCreatorId));

        getChatMembersContainer(chatId)
            .ifPresent(cont -> cont.updateChatCreator(newCreatorId));
    }
    public void deleteAdminRights(Long chatId) {
        getChatMembersContainer(chatId).ifPresent(ChatMembersContainer::deleteAdminRightsForAllAdmins);
    }


    // Вспомогательные методы
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.isAdmin(userId));
    }
    public Optional<List<CacheChatMember>> getChatAdmins(Long chatId) {
        return getChatMembersContainer(chatId).map(ChatMembersContainer::getChatAdmins);
    }
    public Optional<Long> getAnotherChatAdminId(Long chatId, Long exceptUserId) {
        return getChatMembersContainer(chatId).flatMap(cont -> cont.getAnotherAdmin(exceptUserId));
    }


    // ========== CACHE STATISTICS AND MANAGEMENT ==========


    // Основные методы
    public CacheStats getCacheStatus() {
        Map<Long, CacheUser> userCacheSnapshot = userCache.asMap();
        Map<Long, CacheChat> chatInfoCacheSnapshot = chatInfoCache.asMap();
        Map<Long, ChatMembersContainer> containersSnapshot = chatMembersContainerCache.asMap();

        long activatedUserCount = userCacheSnapshot.values().stream()
                .filter(user -> !user.getIsDeleted() && user.getIsEnabled())
                .count();

        int totalUserChats = userCacheSnapshot.values().stream()
                .filter(user -> !user.getIsDeleted() && user.getIsEnabled())
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
            chatInfoCacheSnapshot.values().stream().filter(chat -> !chat.getIsDeleted()).count(),
            chatInfoCacheSnapshot.size(),
            totalUserChats,
            totalChatMembers,
            totalAdminRights,
            (int)verificationTokenCache.estimatedSize(),
            totalDeletedMembers
        );
    }
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
    public Map<String, Object> getDetailedCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        // статистика кеша пользователей
        var userStats = userCache.stats();
        stats.put("userCache.estimatedSize", userCache.estimatedSize());
        stats.put("userCache.hitRate", userStats.hitRate());
        stats.put("userCache.missRate", userStats.missRate());
        stats.put("userCache.evictionCount", userStats.evictionCount());

        var usersPaginationStats = usersPaginationCache.stats();
        stats.put("usersPaginationCache.estimatedSize", usersPaginationCache.estimatedSize());
        stats.put("usersPaginationCache.hitRate", usersPaginationStats.hitRate());
        stats.put("usersPaginationCache.missRate", usersPaginationStats.missRate());
        stats.put("usersPaginationCache.evictionCount", usersPaginationStats.evictionCount());
        stats.put("usersPaginationIndex.size", usersPaginationIndex.size());

        var userChatsPaginationStats = userChatsPaginationCache.stats();
        stats.put("userChatsPaginationCache.estimatedSize", userChatsPaginationCache.estimatedSize());
        stats.put("userChatsPaginationCache.hitRate", userChatsPaginationStats.hitRate());
        stats.put("userChatsPaginationCache.missRate", userChatsPaginationStats.missRate());
        stats.put("userChatsPaginationCache.evictionCount", userChatsPaginationStats.evictionCount());
        stats.put("userChatsPaginationIndex.size", userChatsPaginationIndex.size());

        // статистика кеша чатов
        var chatStats = chatInfoCache.stats();
        stats.put("chatCache.estimatedSize", chatInfoCache.estimatedSize());
        stats.put("chatCache.hitRate", chatStats.hitRate());
        stats.put("chatCache.missRate", chatStats.missRate());
        stats.put("chatCache.evictionCount", chatStats.evictionCount());

        var containerStats = chatMembersContainerCache.stats();
        stats.put("chatContainerCache.estimatedSize", chatMembersContainerCache.estimatedSize());
        stats.put("chatContainerCache.hitRate", containerStats.hitRate());
        stats.put("chatContainerCache.missRate", containerStats.missRate());
        stats.put("chatContainerCache.evictionCount", containerStats.evictionCount());

        var tokenStats = verificationTokenCache.stats();
        stats.put("tokenCache.estimatedSize", verificationTokenCache.estimatedSize());
        stats.put("tokenCache.hitRate", tokenStats.hitRate());
        stats.put("tokenCache.missRate", tokenStats.missRate());
        stats.put("tokenCache.evictionCount", tokenStats.evictionCount());

        // статистика кеша индексов
        stats.put("usernameIndex.size", usernameIndex.size());
        stats.put("emailIndex.size", emailIndex.size());
        stats.put("personalChatIndex.size", personalChatIndex.size());

        return stats;
    }


    // ========== HELPFUL FUNCTIONS ==========


    // Основные методы
    private String getPersonalChatKey(Long userId1, Long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }
    private String getUsersPaginationKey(String filter, int offset, int limit) {
        String normalizedFilter = filter == null ? "" : filter.toLowerCase().trim();
        return normalizedFilter + ":" + offset + ":" + limit;
    }
    private String getUserChatsPaginationKey(long userId, int offset, int limit) {
        return userId + ":" + offset + ":" + limit;
    }
    private String getChatMembersPaginationKey(long chatId, int offset, int limit) {
        return chatId + ":" + offset + ":" + limit;
    }
}