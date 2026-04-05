package com.sunrise.core.dataservice;

import com.sunrise.entity.cache.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
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


    // контейнеры участников чата
    private final Cache<Long, CacheChatMembersContainer> chatMembersContainerCache = Caffeine.newBuilder() // chatId -> CacheChatMembersContainer (контейнер с участниками чата)
            .maximumSize(200_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<Long, CacheMessage> messageCache = Caffeine.newBuilder() // messageId -> CacheMessage (сообщения)
            .maximumSize(300_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    private final Cache<String, Long> lastReadCache = Caffeine.newBuilder() // "chatId:userId" -> messageId (для быстрой проверки на прочтение)
            .maximumSize(300_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();


    // кеш токенов подтверждения
    private final Cache<String, CacheVerificationToken> verificationTokenCache = Caffeine.newBuilder() // token -> CacheVerificationToken (токены подтверждения)
            .maximumSize(50_000)
            .expireAfterWrite(2, TimeUnit.HOURS)
            .build();



    // ========== USER METHODS ==========


    // Основные методы
    public void saveUsers(Collection<CacheUser> users) {
        for (CacheUser user : users){
            Optional<CacheUser> existing = getUserLink(user.getId());
            if (existing.isPresent()) {
                existing.get().updateFromCache(user);
            } else {
                userCache.put(user.getId(), CacheUser.copy(user));
            }

            usernameIndex.put(user.getUsername().toLowerCase(), user.getId());
            emailIndex.put(user.getEmail().toLowerCase(), user.getId());
        }
        log.debug("[⚡] Batch saved {} users to cache and updated indexes || saveUsers", users.size());
    }
    public void saveUser(CacheUser user) {
        Optional<CacheUser> existing = getUserLink(user.getId());
        if (existing.isPresent()) {
            existing.get().updateFromCache(user);
        } else {
            userCache.put(user.getId(), CacheUser.copy(user));
        }

        usernameIndex.put(user.getUsername().toLowerCase(), user.getId());
        emailIndex.put(user.getEmail().toLowerCase(), user.getId());
        log.debug("[⚡] Saved user {} in cache and updated indexes || saveUser", user.getId());
    }
    public void deleteUser(long userId) {
        getUserLink(userId).ifPresent(cacheUser -> {
            cacheUser.setDeleted(true);
            log.debug("[⚡] Marked user {} as deleted in cache || deleteUser", userId);
        });
    }
    public void restoreUser(long userId) {
        getUserLink(userId).ifPresent(cacheUser -> {
            cacheUser.setDeleted(false);
            log.debug("[⚡] Restored user {} in cache || restoreUser", userId);
        });
    }

    // Вспомогательные методы
    public void updateUserIsEnabled(long userId, boolean isEnabled) {
        getUserLink(userId).ifPresent(cacheUser -> {
            cacheUser.setEnabled(isEnabled);
            log.debug("[⚡] Updated user {} enabled status to {} in cache || updateUserIsEnabled", userId, isEnabled);
        });
    }
    public void updateUserLastLogin(String username, LocalDateTime lastLogin) {
        String key = username.toLowerCase();
        Long userId = usernameIndex.getIfPresent(key);
        if (userId == null) return;

        getUserLink(userId).ifPresent(user -> {
            user.setLastLogin(lastLogin);
            log.debug("[⚡] Updated last login for user {} to {} || updateUserLastLogin", user.getId(), lastLogin);
        });
    }
    public void updateUserProfile(long userId, String username, String name) {
        getUserLink(userId).ifPresent(user -> {
            // Обновляем username в индексе
            String oldUsername = user.getUsername();
            if (!oldUsername.equals(username)) {
                usernameIndex.invalidate(oldUsername.toLowerCase());
                usernameIndex.put(username.toLowerCase(), userId);
                log.debug("[⚡] Updated username index: {} -> {} for user {}", oldUsername, username, userId);
            }

            // Обновляем данные пользователя
            user.setUsername(username);
            user.setName(name);
            log.debug("[⚡] Updated profile for user {}: username={}, name={} || updateUserProfile", userId, username, name);
        });
    }

    public Map<Long, CacheUser> getCacheUsersByIds(Collection<Long> userIds, Collection<Long> missingIds) {
        Map<Long, CacheUser> result = new HashMap<>(userIds.size());
        for (Long userId : userIds) {
            Optional<CacheUser> user = getUser(userId);
            if (user.isPresent()) {
                result.put(userId, user.get());
            } else if (missingIds != null) {
                missingIds.add(userId);
            }
        }
        return result;
    }
    public Optional<CacheUser> getUser(long userId) {
        return Optional.ofNullable(CacheUser.copy(userCache.getIfPresent(userId)));
    }
    public Optional<CacheUser> getUserByUsername(String username) {
        String key = username.toLowerCase();
        Long userId = usernameIndex.getIfPresent(key);
        if (userId == null) return Optional.empty();

        Optional<CacheUser> user = getUser(userId);
        if (user.isEmpty()) {
            usernameIndex.invalidate(key);
        }
        return user;
    }
    private Optional<CacheUser> getUserLink(long userId) {
        return Optional.ofNullable(userCache.getIfPresent(userId));
    }

    public boolean existsUser(long userId) {
        return getUserLink(userId).isPresent();
    }
    public boolean existsUserByUsername(String username) {
        return usernameIndex.getIfPresent(username.toLowerCase()) != null;
    }
    public boolean existsUserByEmail(String email) {
        return emailIndex.getIfPresent(email.toLowerCase()) != null;
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChats(Collection<CacheChat> newChats) {
        for (CacheChat newChat : newChats){
            Optional<CacheChat> oldChat = getChatLink(newChat.getId());
            if (oldChat.isPresent()) {
                oldChat.get().updateFromCache(newChat);
            } else {
                chatInfoCache.put(newChat.getId(), CacheChat.copy(newChat));
                if(!newChat.isGroup()) {
                    savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
                }
            }
        }
        log.debug("[⚡] Batch saved {} chats to cache and updated indexes || saveChats", newChats.size());
    }
    public void saveChat(CacheChat newChat) {
        Optional<CacheChat> oldChat = getChatLink(newChat.getId());
        if (oldChat.isPresent()) {
            oldChat.get().updateFromCache(newChat);
        } else {
            chatInfoCache.put(newChat.getId(), CacheChat.copy(newChat));
            if(!newChat.isGroup()) {
                savePersonalChatIndex(newChat.getId(), newChat.getCreatedBy(), newChat.getOpponentId());
            }
        }
        log.debug("[⚡] Saved chat {} in cache and updated indexes || saveChat", newChat.getId());
    }
    public void updateChatCreator(long chatId, long newCreatorId) {
        getChatLink(chatId).ifPresent(chat -> {
            chat.setCreatedBy(newCreatorId);
            log.debug("[⚡] Updated chat {} creator to {} || updateChatCreator", chatId, newCreatorId);
        });

        getChatMembersContainer(chatId).ifPresent(container -> {
            container.updateChatCreator(newCreatorId);
            log.debug("[⚡] Updated members container for chat {} with new creator {} || updateChatCreator", chatId, newCreatorId);
        });
    }
    public void deleteChat(long chatId) {
        getChatLink(chatId).ifPresent(chat -> {
            chat.delete();
            log.debug("[⚡] Marked chat {} as deleted in cache || deleteChat", chatId);
        });
    }
    public void restoreChat(long chatId) {
        getChatLink(chatId).ifPresent(chat -> {
            chat.restore();
            log.debug("[⚡] Restored chat {} in cache || restoreChat", chatId);
        });
    }


    // Вспомогательные методы
    public Optional<CacheChat> getChat(long chatId) {
        return Optional.ofNullable(CacheChat.copy(chatInfoCache.getIfPresent(chatId)));
    }
    public Optional<CacheChat> getPersonalChat(long userId1, long userId2) {
        String key = getPersonalChatKey(userId1, userId2);
        Long chatId = personalChatIndex.getIfPresent(key);
        if (chatId == null) return Optional.empty();

        Optional<CacheChat> chat = getChat(chatId);
        if (chat.isEmpty()) personalChatIndex.invalidate(key);
        return chat;
    }
    private Optional<CacheChat> getChatLink(long chatId) {
        return Optional.ofNullable(chatInfoCache.getIfPresent(chatId));
    }

    public Optional<Boolean> isActiveChat(long chatId) {
        return getChatLink(chatId).map(CacheChat::isActive);
    }
    public Optional<Boolean> isActiveGroupChat(long chatId) {
        return getChatLink(chatId).filter(CacheChat::isActive).map(CacheChat::isGroup);
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
    private CacheChatMembersContainer getOrCreateChatMembersContainer(long chatId) {
        return chatMembersContainerCache.get(chatId, key -> new CacheChatMembersContainer(chatId));
    }
    private Optional<CacheChatMembersContainer> getChatMembersContainer(long chatId) {
        return Optional.ofNullable(chatMembersContainerCache.getIfPresent(chatId));
    }

    public void saveChatMembers(long chatId, Collection<CacheChatMember> members) {
        // Обновляем контейнер
        getOrCreateChatMembersContainer(chatId).addMembers(members);
        log.debug("[⚡] Batch saved {} chat members in chat {} || saveChatMember", members.size(), chatId);
    }
    public void saveChatMember(CacheChatMember chatMember) {
        long chatId = chatMember.getChatId();
        long userId = chatMember.getUserId();

        // Обновляем контейнер
        getOrCreateChatMembersContainer(chatId).addMember(chatMember);
        log.debug("[⚡] Saved chat member {} in chat {} || saveChatMember", userId, chatId);
    }
    public void updateAdminRights(long chatId, long userId, boolean isAdmin) {
        getChatMembersContainer(chatId).ifPresent(cont -> {
            cont.updateAdminRights(userId, isAdmin);
            log.debug("[⚡] Updated admin rights for member {} in chat {} || updateAdminRights", userId, chatId);
        });
    }
    public void removeChatMember(long userId, long chatId) {
        // Обновляем контейнер
        getChatMembersContainer(chatId).ifPresent(c -> {
            c.markMemberAsDeleted(userId);
            log.debug("[⚡] Marked member {} as deleted in chat {} || removeChatMember", userId, chatId);
        });
    }
    public void restoreChatMember(long userId, long chatId, boolean isAdmin) {
        // Обновляем контейнер
        getChatMembersContainer(chatId).ifPresent(c -> {
            c.restoreMember(userId, isAdmin);
            log.debug("[⚡] Restored member {} in chat {} (isAdmin={}) || restoreChatMember", userId, chatId, isAdmin);
        });
    }


    // Вспомогательные методы
    public Map<Long, CacheChatMember> getChatMembers(long chatId, Collection<Long> userIds, Collection<Long> missingIds) {
        Map<Long, CacheChatMember> result = new HashMap<>(userIds.size());

        Optional<CacheChatMembersContainer> containerOpt = getChatMembersContainer(chatId);
        if (containerOpt.isPresent()) {
            CacheChatMembersContainer container = containerOpt.get();
            for (Long userId : userIds) {
                Optional<CacheChatMember> member = container.getMember(userId);
                if (member.isPresent()) {
                    result.put(userId, member.get());
                } else if (missingIds != null) {
                    missingIds.add(userId);
                }
            }
        } else if (missingIds != null) {
            missingIds.addAll(userIds);
        }

        return result;
    }
    public Optional<CacheChatMember> getChatMember(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.getMember(userId));
    }
    public Optional<Boolean> hasActiveChatMember(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.hasMemberAndIsActive(userId));
    }



    // Вспомогательные методы
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        return getChatMembersContainer(chatId).flatMap(c -> c.isAdmin(userId));
    }
    public Optional<List<CacheChatMember>> getChatAdmins(long chatId) {
        return getChatMembersContainer(chatId).map(CacheChatMembersContainer::getChatAdmins);
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(CacheVerificationToken cache) {
        verificationTokenCache.put(cache.getToken(), CacheVerificationToken.copy(cache));
        log.debug("[⚡] Saved verification token for user {} (token={}) || saveVerificationToken", cache.getUserId(), cache.getToken());
    }
    public void deleteVerificationToken(String token) {
        verificationTokenCache.invalidate(token);
        log.debug("[⚡] Deleted verification token {} || deleteVerificationToken", token);
    }

    // Вспомогательные методы
    public Optional<CacheVerificationToken> getVerificationToken(String token) {
        return Optional.ofNullable(CacheVerificationToken.copy(verificationTokenCache.getIfPresent(token)));
    }



    // ========== MESSAGES METHODS ==============


    // Основные методы
    public void saveNewMessage(CacheMessage message) {
        CacheMessage copy = CacheMessage.copy(message);
        messageCache.put(copy.getId(), copy);
        log.debug("[⚡] Saved new message {} in cache (chat={}, sender={}) || saveNewMessage", message.getId(), message.getChatId(), message.getSenderId());

        getChatLink(copy.getChatId()).ifPresent(chat -> {
            chat.setNewestMessage(copy);
            log.debug("[⚡] Updated newest message for chat {} to {} || saveNewMessage", message.getChatId(), message.getId());
        });
    }
    public void saveMessage(CacheMessage message) {
        CacheMessage copy = CacheMessage.copy(message);
        messageCache.put(copy.getId(), copy);
        log.debug("[⚡] Saved message {} in cache (chat={}, sender={}) || saveMessage", copy.getId(), copy.getChatId(), copy.getSenderId());
    }
    public void saveMessages(List<CacheMessage> messages) {
        for (CacheMessage message : messages) {
            messageCache.put(message.getId(), CacheMessage.copy(message));
        }
        log.debug("[⚡] Batch saved {} messages to cache || saveMessages", messages.size());
    }
    public void markMessageAsRead(long chatId, long userId, long messageId) {
        String key = getLastReadKey(chatId, userId);
        Long[] oldValueHolder = new Long[1];

        Long newValue = lastReadCache.asMap().compute(key, (k, oldValue) -> {
            oldValueHolder[0] = oldValue;
            if (oldValue == null) return messageId;
            return Math.max(oldValue, messageId);
        });

        boolean wasUpdated = (oldValueHolder[0] == null) || (newValue > oldValueHolder[0]);
        if (wasUpdated && newValue == messageId) {
            getMessageLink(messageId).ifPresent(CacheMessage::incrementReadCount);
        }
    }
    public void restoreMessage(long chatId, long messageId) {
        getMessageLink(messageId).ifPresent(message -> {
            message.restore();
            log.debug("[⚡] Restored message {} in cache (chat={}) || restoreMessage", messageId, chatId);
        });

        getChatLink(chatId).ifPresent(chat -> {
            chat.updateNewestMessageIsDeletedIfHasId(messageId, false);
            log.debug("[⚡] Updated newest message status for chat {} (message {}) || restoreMessage", chatId, messageId);
        });
    }
    public void deleteMessage(long chatId, long messageId) {
        getMessageLink(messageId).ifPresent(message -> {
            message.delete();
            log.debug("[⚡] Deleted message {} in cache (chat={}) || deleteMessage", messageId, chatId);
        });

        getChatLink(chatId).ifPresent(chat -> {
            chat.updateNewestMessageIsDeletedIfHasId(messageId, true);
            log.debug("[⚡] Updated newest message as deleted for chat {} (message {}) || deleteMessage", chatId, messageId);
        });
    }

    // Вспомогательные методы
    public Map<Long, CacheMessage> getMessages(Collection<Long> messageIds, Collection<Long> missingIds) {
        Map<Long, CacheMessage> result = new HashMap<>(messageIds.size());
        for (Long messageId : messageIds) {
            CacheMessage user = messageCache.getIfPresent(messageId);
            if (user != null) {
                result.put(messageId, user);
            } else if (missingIds != null) {
                missingIds.add(messageId);
            }
        }
        return result;
    }
    public Optional<CacheMessage> getMessage(long messageId) {
        return Optional.ofNullable(CacheMessage.copy(messageCache.getIfPresent(messageId)));
    }
    private Optional<CacheMessage> getMessageLink(long messageId) {
        return Optional.ofNullable(messageCache.getIfPresent(messageId));
    }


    // ========== МЕТОДЫ ДЛЯ ПРОЧТЕНИЯ ==========


    // Основные методы
    private String getLastReadKey(long chatId, long userId) {
        return chatId + ":" + userId;
    }
    public void updateLastReadByUser(long chatId, long userId, long messageId) {
        String key = getLastReadKey(chatId, userId);

        lastReadCache.asMap().compute(key, (k, oldValue) -> {
            if (oldValue == null) {
                return messageId;
            }
            return Math.max(oldValue, messageId);
        });
    }
    public Long getUserLastRead(long chatId, long userId) {
        String key = getLastReadKey(chatId, userId);
        return lastReadCache.getIfPresent(key);
    }




    // ========== CACHE STATISTICS AND MANAGEMENT ==========


    // Основные методы
    @Data
    @AllArgsConstructor
    public static final class CacheStats {
        final long activatedUserCount;
        final int allUserCount;
        final long notDeletedChatCount;
        final int chatCount;
        final int chatMembersCount;
        final int adminRightsCount;
        final int verificationTokenCount;
        final int deletedMembersCount;
    }

    public CacheStats getCacheStatus() {
        Map<Long, CacheUser> userCacheSnapshot = userCache.asMap();
        Map<Long, CacheChat> chatInfoCacheSnapshot = chatInfoCache.asMap();
        Map<Long, CacheChatMembersContainer> containersSnapshot = chatMembersContainerCache.asMap();

        long activatedUserCount = userCacheSnapshot.values().stream()
                .filter(user -> !user.isDeleted() && user.isEnabled())
                .count();

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
            totalChatMembers,
            totalAdminRights,
            (int)verificationTokenCache.estimatedSize(),
            totalDeletedMembers
        );
    }
    public void printCacheStats() {
        CacheService.CacheStats stats = getCacheStatus();
        log.info("📊 Cache Statistics:");
        log.info("   ├─ Active Users: {}", stats.getActivatedUserCount());
        log.info("   ├─ Users: {}", stats.getAllUserCount());
        log.info("   ├─ Active Chats: {}", stats.getChatCount());
        log.info("   ├─ Verification Tokens: {}", stats.getVerificationTokenCount());
        log.info("   ├─ Chat Members: {}", stats.getChatMembersCount());
        log.info("   └─ Admin Rights: {}", stats.getAdminRightsCount());
    }

    public Map<String, Object> getDetailedCacheStatus() {
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
}