package com.Sunrise.Services.DataServices;

import com.Sunrise.Entities.User;
import com.Sunrise.Entities.Chat;
import com.Sunrise.Entities.VerificationToken;
import com.Sunrise.Services.DataServices.CacheEntities.CacheChat;
import com.Sunrise.Services.DataServices.CacheEntities.CacheChatMember;
import com.Sunrise.Services.DataServices.CacheEntities.CacheUser;
import com.Sunrise.Services.DataServices.CacheEntities.FullChatMember;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class CacheService {

    // Основные кеши
    private final Map<Long, CacheUser> userCache = new ConcurrentHashMap<>(); // userId -> CacheUser (пользователи)

    private final Set<Long> notDeletedChatIds = ConcurrentHashMap.newKeySet(); // chatId
    private final Map<Long, CacheChat> chatInfoCache = new ConcurrentHashMap<>(); // chatId -> CacheChat (чаты)

    private final Map<String, VerificationToken> verificationTokenCache = new ConcurrentHashMap<>(); // token -> VerificationToken (токены)

    // Связующие кеши
    private final Map<String, Long> personalChatCache = new ConcurrentHashMap<>(); // "userId_1:userId_2" -> chatId (личные чаты)


    // ========== USER METHODS ==========

    // Основные методы
    public void saveUser(User user) {
        if (user != null && user.getId() != null)
            userCache.put(user.getId(), new CacheUser(user));
    }
    public void deleteUser(Long userId) {
        userCache.computeIfPresent(userId, (id, cacheUser) -> {
            cacheUser.setIsDeleted(true);
            return cacheUser;
        });
    }
    public void restoreUser(Long userId) {
        userCache.computeIfPresent(userId, (id, cacheUser) -> {
            cacheUser.setIsDeleted(false);
            return cacheUser;
        });
    }

    // Вспомогательные методы
    public void enableUser(Long userId){
        userCache.computeIfPresent(userId, (id, cacheChat) -> {
            cacheChat.setIsEnabled(true);
            return cacheChat;
        });
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        if (username != null && lastLogin != null)
            userCache.values().stream().filter(user -> username.equals(user.getUsername()))
                .findFirst().ifPresent(user -> user.setLastLogin(lastLogin));
    }
    public Optional<User> getUser(Long userId) {
        return Optional.ofNullable(userCache.get(userId));
    }
    public Optional<CacheUser> getCacheUser(Long userId) {
        return Optional.ofNullable(userCache.get(userId));
    }
    public Optional<Set<FullChatMember>> getFullChatMembers(Long chatId){
        Set<FullChatMember> fullChatMembers = new HashSet<>();

        CacheChat chat = chatInfoCache.get(chatId);
        if (chatInfoCache.get(chatId) == null)
            return Optional.empty();

        for (Long memberId : getChatMembers(chatId)){
            CacheChatMember chatMember = chat.getMemberInfo(memberId);
            Optional<CacheUser> user = getCacheUser(chatMember.getUserId());
            if (user.isEmpty())
                return Optional.empty();

            fullChatMembers.add(FullChatMember.create(user.get(), chatMember));
        }

        return Optional.of(fullChatMembers);
    }
    public Optional<User> getUserByUsername(String username) {
        return userCache.values().stream().filter(user -> user.getUsername().equalsIgnoreCase(username)).findFirst().map(user -> (User)user);
    }
    public List<User> getFilteredUsers(String filter, int limit, int offset) {
        Stream<CacheUser> userStream = userCache.values().stream()
                .filter(user -> !user.getIsDeleted() && user.getIsEnabled()); // только активные и не удаленные

        if (filter != null && !filter.trim().isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            userStream = userStream.filter(user ->
                user.getUsername().toLowerCase().contains(lowerFilter) ||
                user.getName().toLowerCase().contains(lowerFilter)
            );
        }

        return userStream.skip(offset).limit(limit).map(user -> (User) user).toList();
    }
    public boolean existsUser(Long userId) {
        return userCache.containsKey(userId);
    }
    public Boolean existsUserByUsername(String username) {
        return userCache.values().stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
    }
    public Boolean existsUserByEmail(String email) {
        return userCache.values().stream().anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChat(Chat chat) {
        if (chat != null && chat.getId() instanceof Long id) {
            CacheChat cacheChat = new CacheChat(chat);
            chatInfoCache.put(id, cacheChat);

            if (!cacheChat.getIsDeleted())
                notDeletedChatIds.add(id);
        }
    }
    public void deleteChat(Long chatId) {
        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            cacheChat.setIsDeleted(true);
            notDeletedChatIds.remove(chatId);
            return cacheChat;
        });
//        personalChatCache.entrySet().removeIf(entry -> entry.getValue().equals(chatId));
    }
    public void restoreChat(Long chatId) {
        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            cacheChat.setIsDeleted(false);
            notDeletedChatIds.add(chatId);
            return cacheChat;
        });
    }

    // Вспомогательные методы
    public Optional<Boolean> isGroupChat(Long chatId) {
        if (!notDeletedChatIds.contains(chatId))
            return Optional.empty();

        return Optional.ofNullable(chatInfoCache.get(chatId)).map(CacheChat::getIsGroup);
    }
    public boolean existsChat(Long chatId) {
        return notDeletedChatIds.contains(chatId);
    }
    public Optional<CacheChat> getChatInfo(Long chatId) {
        if (!notDeletedChatIds.contains(chatId))
            return Optional.empty();

        return Optional.ofNullable(chatInfoCache.get(chatId));
    }
    public Optional<String> getChatName(Long chatId) {
        if (!notDeletedChatIds.contains(chatId))
            return Optional.empty();

        return Optional.ofNullable(chatInfoCache.get(chatId)).map(CacheChat::getName);
    }
    public Optional<Long> getChatCreator(Long chatId) {
        if (!notDeletedChatIds.contains(chatId))
            return Optional.empty();

        return Optional.ofNullable(chatInfoCache.get(chatId)).map(CacheChat::getCreatedBy);
    }

    // Методы для работы с личными чатами
    public void makePersonalChatCache(Chat chat, Long userId2) {
        Long id = chat.getId();
        Long createdBy = chat.getCreatedBy();

        // Сохраняем в кеш
        CacheChat cacheChat = new CacheChat(id, null, createdBy, false);

        makeChatCache(cacheChat);
        makePersonalChatCache(createdBy, userId2, id);

        addUserToChat(id, createdBy, true);
        addUserToChat(id, userId2, true);
    }
    public Optional<Long> findExistingPersonalChat(Long userId1, Long userId2) {
        String key = getPersonalChatKey(userId1, userId2);

        if (personalChatCache.get(key) instanceof Long chatId) {
            if (chatInfoCache.get(chatId) instanceof CacheChat chat && !chat.getIsDeleted())
                return Optional.of(chatId);
        }
        return Optional.empty();
    }
    public Optional<Long> findDeletedPersonalChat(Long userId1, Long userId2) {
        String key = getPersonalChatKey(userId1, userId2);

        if (personalChatCache.get(key) instanceof Long chatId) {
            if (chatInfoCache.get(chatId) instanceof CacheChat chat && chat.getIsDeleted())
                return Optional.of(chatId);
            System.out.println(key);
        }
        return Optional.empty();
    }
    public void makePersonalChatCache(Long userId1, Long userId2, Long chatId) {
        personalChatCache.put(getPersonalChatKey(userId1, userId2), chatId);
    }

    // Методы для работы с групповыми чатами
    public void saveGroupChat(Chat chat, Set<Long> usersId) {
        Long id = chat.getId();
        Long createdBy = chat.getCreatedBy();

        // Сохраняем в кеш
        CacheChat cacheChat = new CacheChat(id, chat.getName(), createdBy, true);

        makeChatCache(cacheChat);

        addUserToChat(id, createdBy, true);
        for(Long userId : usersId)
            addUserToChat(id, userId, false);
    }

    public void makeChatCache(CacheChat cacheChat){
        chatInfoCache.put(cacheChat.getId(), cacheChat);
        notDeletedChatIds.add(cacheChat.getId());
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void addUserToChat(Long chatId, Long userId, Boolean isAdmin) {
        userCache.computeIfPresent(userId, (id, cacheUser) -> {
            cacheUser.addChat(chatId);
            return cacheUser;
        });
        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            cacheChat.addMember(userId, isAdmin);
            return cacheChat;
        });
    }
    public void removeUserFromChat(Long userId, Long chatId) {
        userCache.computeIfPresent(userId, (id, cacheUser) -> {
            cacheUser.removeChat(chatId);
            return cacheUser;
        });

        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            cacheChat.removeMember(userId);
            return cacheChat;
        });

        deleteAdminRights(chatId, userId);
    }

    // Вспомогательные методы
    public Optional<List<Long>> getUserChats(Long userId) {
        return getCacheUser(userId)
                .filter(user -> !user.getIsDeleted() && user.getIsEnabled())
                .map(cacheUser -> {
                    Set<Long> userChats = cacheUser.getChats();
                    List<Long> activeChats = new ArrayList<>(userChats.size());

                    for (Long chatId : userChats) {
                        if (notDeletedChatIds.contains(chatId))
                            activeChats.add(chatId);
                    }
                    return activeChats;
                });
    }
    public Set<Long> getChatMembers(Long chatId) {
        if (!notDeletedChatIds.contains(chatId))
            return Collections.emptySet();

        return chatInfoCache.get(chatId) instanceof CacheChat chat ? chat.getActiveMemberIds() : Collections.emptySet();
    }
    public boolean isUserInChat(Long chatId, Long userId) {
        if (!notDeletedChatIds.contains(chatId))
            return false;

        return getCacheUser(userId).filter(user -> !user.getIsDeleted() && user.getIsEnabled())
                .map(cacheUser -> cacheUser.hasChat(chatId)).orElse(false);
    }


    // ========== VERIFICATION TOKEN METHODS ==========


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
        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            if (userId.equals(cacheChat.getCreatedBy()) && !isAdmin)
                return cacheChat; // Игнорируем попытку снять права у создателя

            if (cacheChat.hasActiveMember(userId))
                cacheChat.setAdminRights(userId, isAdmin);
            return cacheChat;
        });
    }
    public void deleteAdminRights(Long chatId, Long userId) {
        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            if (!userId.equals(cacheChat.getCreatedBy()))
                cacheChat.setAdminRights(userId, false);
            return cacheChat;
        });
    }

    // Вспомогательные методы
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        if (!notDeletedChatIds.contains(chatId))
            return Optional.empty();

        return Optional.ofNullable(chatInfoCache.get(chatId)).map(cacheChat -> cacheChat.isAdmin(userId));
    }
    public Set<Long> getChatAdmins(Long chatId) {
        if (!notDeletedChatIds.contains(chatId))
            return Collections.emptySet();

        return chatInfoCache.get(chatId) instanceof CacheChat cacheChat ? cacheChat.getAdminIds() : Collections.emptySet();
    }
    public void clearAdminRightsForChat(Long chatId) {
        chatInfoCache.computeIfPresent(chatId, (id, cacheChat) -> {
            if(cacheChat.getIsDeleted())
                return cacheChat;

            Long creatorId = cacheChat.getCreatedBy();
            cacheChat.getActiveMemberIds().forEach(memberId -> {
                if (!memberId.equals(creatorId)) // Не сбрасываем права создателя
                    cacheChat.setAdminRights(memberId, false);
            });
            return cacheChat;
        });
    }
    public void clearAdminRightsForUser(Long userId) {
        chatInfoCache.values().forEach(cacheChat -> {
            if (!cacheChat.getIsDeleted() && cacheChat.hasActiveMember(userId) && !userId.equals(cacheChat.getCreatedBy())) // Не сбрасываем права создателя
                cacheChat.setAdminRights(userId, false);
        });
    }


    // ========== CACHE STATISTICS AND MANAGEMENT ==========


    // Основные методы
    public CacheStats getStats() {
        int activatedUserCount = (int)userCache.values().stream().filter(user -> !user.getIsDeleted() && user.getIsEnabled()).count();
        int totalUserChats = userCache.values().stream().filter(user -> !user.getIsDeleted() && user.getIsEnabled()).mapToInt(CacheUser::getChatsCount).sum();
        int totalChatMembers = chatInfoCache.values().stream().filter(chat -> !chat.getIsDeleted()).mapToInt(CacheChat::getActiveMemberCount).sum();
        int totalAdminRights = chatInfoCache.values().stream().filter(chat -> !chat.getIsDeleted())
                .mapToInt(cacheChat -> (int)cacheChat.getChatMembers().values().stream().filter(CacheChatMember::getIsAdmin).count())
                .sum();

        return new CacheStats(
            activatedUserCount,
            0,
            userCache.size(),
            notDeletedChatIds.size(),
            chatInfoCache.size(),
            totalUserChats,
            totalChatMembers,
            totalAdminRights,
            verificationTokenCache.size()
        );
    }
    public record CacheStats(int activatedUserCount, int activeUserCount, int userCount, int notDeletedChatCount, int chatCount, int userChatsCount, int chatMembersCount, int adminRightsCount, int verificationTokenCount) { }


    // ========== HELPFUL FUNCTIONS ==========


    // Основные методы
    private String getPersonalChatKey(Long userId1, Long userId2) {
        return Math.min(userId1, userId2) + ":" + Math.max(userId1, userId2);
    }
}