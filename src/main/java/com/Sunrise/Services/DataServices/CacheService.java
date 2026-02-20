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
import org.springframework.scheduling.annotation.Scheduled;
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

    private final Cache<String, UsersPagination> usersPaginationCache = Caffeine.newBuilder()// "filter:offset:limit" -> paginationId || для getFilteredUsers (Это можно будет заменить потом на ElasticSearch)
            .maximumSize(5_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
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
            .softValues()
            .recordStats()
            .build();

    private final Cache<String, UserChatsPagination> userChatsPaginationCache = Caffeine.newBuilder() // "userId:offset:limit" -> UserChatsPagination
            .maximumSize(10_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();

    private final Map<Long, Set<String>> userPaginationKeys = new ConcurrentHashMap<>(); // userId -> Set<UserChatsPaginationKey>


    // контейнеры участников чата
    private final Cache<Long, ChatMembersContainer> chatMembersContainerCache = Caffeine.newBuilder()
            .maximumSize(200_000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, ChatMembersPagination> chatMembersPaginationCache = Caffeine.newBuilder() // "chatId:offset:limit" -> paginationId
            .maximumSize(50_000)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .recordStats()
            .build();


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
        getUserByUsername(username).ifPresent(user -> user.setLastLogin(lastLogin));
    }

    public Optional<CacheUser> getCacheUser(Long userId) {
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
    public Optional<CacheUser> getUserByEmail(String email) {
        String key = email.toLowerCase();
        Long userId = emailIndex.getIfPresent(key);
        if (userId == null) return Optional.empty();

        Optional<CacheUser> user = getCacheUser(userId);
        if (user.isEmpty()) {
            emailIndex.invalidate(key);
        }
        return user;
    }
    public boolean existsUser(Long userId) {
        return getCacheUser(userId).isPresent();
    }
    public Boolean existsUserByUsername(String username) {
        return usernameIndex.getIfPresent(username.toLowerCase()) != null;
    }
    public Boolean existsUserByEmail(String email) {
        return emailIndex.getIfPresent(email.toLowerCase()) != null;
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
        Long chatId = personalChatIndex.getIfPresent(key);
        if (chatId == null) return Optional.empty();

        CacheChat chat = chatInfoCache.getIfPresent(chatId);
        if (chat == null) {
            personalChatIndex.invalidate(key);
        }
        return Optional.ofNullable(chat);
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
    private String getPersonalChatKey(Long userId1, Long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }
    public void savePersonalChatIndex(Long chatId, Long creatorId, Long userId2) {
        personalChatIndex.put(getPersonalChatKey(creatorId, userId2), chatId);
    }


    // cache методы
    public void invalidateAfterChatAdded(List<Long> membersIds) {
        membersIds.forEach(this::invalidateUserChatsPagination);
    }
    public void invalidateAfterChatDeleted(List<Long> membersIds) {
        membersIds.forEach(this::invalidateUserChatsPagination);
    }
    public void invalidateAfterChatRestored(List<Long> membersIds) {
        membersIds.forEach(this::invalidateUserChatsPagination);
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
        getOrCreateChatMembersContainer(chat).addNewMembers(cacheMembers);

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


    // ========== PAGINATION METHODS ==========


    // пагинация для выдачи пользователей
    @Builder
    public record UsersPagination(
        long id, String filter,
        int offset, int limit,
        List<Long> userIds,
        LocalDateTime createdAt,
        boolean hasMore, int totalCount) { }

    private String getUsersPaginationKey(String filter, int offset, int limit) {
        String normalizedFilter = filter == null ? "" : filter.toLowerCase().trim();
        return normalizedFilter + ":" + offset + ":" + limit;
    }
    public void saveUsersPagination(UsersPagination pagination) {
        String key = getUsersPaginationKey(pagination.filter(), pagination.offset(), pagination.limit());
        usersPaginationCache.put(key, pagination);
    }
    public Optional<UsersPagination> findUsersPagination(String filter, int offset, int limit) {
        String key = getUsersPaginationKey(filter, offset, limit);
        return Optional.ofNullable(usersPaginationCache.getIfPresent(key));
    }
    public void invalidateUsersPagination() {
        // При изменении пользователей инвалидируем весь кеш поиска
        usersPaginationCache.invalidateAll();
    }


    // пагинация для выдачи чатов пользователей
    @Builder
    public record UserChatsPagination(
            long id, long userId,
            int offset, int limit,
            List<Long> chatIds,
            LocalDateTime createdAt,
            boolean hasMore, int totalCount) { }

    private String getUserChatsPaginationKey(long userId, int offset, int limit) {
        return userId + ":" + offset + ":" + limit;
    }
    public void saveUserChatsPagination(UserChatsPagination pagination) {
        String key = getUserChatsPaginationKey(pagination.userId(), pagination.offset(), pagination.limit());
        userChatsPaginationCache.put(key, pagination);
        userPaginationKeys.computeIfAbsent(pagination.userId(), k -> ConcurrentHashMap.newKeySet()).add(key);
    }
    public Optional<UserChatsPagination> findUserChatsPagination(long userId, int offset, int limit) {
        String key = getUserChatsPaginationKey(userId, offset, limit);
        return Optional.ofNullable(userChatsPaginationCache.getIfPresent(key));
    }
    public void invalidateUserChatsPagination(long userId) {
        Set<String> keys = userPaginationKeys.remove(userId);
        if (keys != null)
            keys.forEach(userChatsPaginationCache::invalidate);
    }


    // пагинация для выдачи участников чата
    @Builder
    public record ChatMembersPagination(
        long id, long chatId,
        int offset, int limit,
        List<Long> memberUserIds,
        LocalDateTime createdAt,
        boolean hasMore, int totalCount) { }

    private String getChatMembersPaginationKey(long chatId, int offset, int limit) {
        return chatId + ":" + offset + ":" + limit;
    }
    public void saveChatMembersPagination(ChatMembersPagination pagination) {
        String key = getChatMembersPaginationKey(pagination.chatId(), pagination.offset(), pagination.limit());
        chatMembersPaginationCache.put(key, pagination);
    }
    public Optional<ChatMembersPagination> findChatMembersPagination(long chatId, int offset, int limit) {
        String key = getChatMembersPaginationKey(chatId, offset, limit);
        return Optional.ofNullable(chatMembersPaginationCache.getIfPresent(key));
    }
    public void invalidateChatMembersPagination(long chatId) {
        chatMembersPaginationCache.asMap().entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(chatId + ":")) {
                chatMembersPaginationCache.invalidate(entry.getKey());
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
        stats.put("usernameIndex.size", usernameIndex.estimatedSize());
        stats.put("emailIndex.size", emailIndex.estimatedSize());
        stats.put("personalChatIndex.size", personalChatIndex.estimatedSize());

        return stats;
    }

    @Scheduled(fixedDelay = 3_600_000) // 1000 * 60 * 60
    public void cleanupUserPaginationKeys() {
        userPaginationKeys.entrySet().removeIf(entry -> {
            // Если пользователь больше не в кеше - чистим его ключи
            if (userCache.getIfPresent(entry.getKey()) == null) {
                entry.getValue().forEach(userChatsPaginationCache::invalidate);
                return true;
            }

            // Удаляем конкретные ключи, которые уже вытеснены из кеша
            entry.getValue().removeIf(key ->
                userChatsPaginationCache.getIfPresent(key) == null
            );

            return entry.getValue().isEmpty();
        });
    }
}