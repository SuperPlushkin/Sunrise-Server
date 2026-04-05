package com.sunrise.core.dataservice;

import com.sunrise.core.dataservice.type.*;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.entity.cache.*;
import com.sunrise.entity.db.*;
import com.sunrise.entity.dto.*;
import com.sunrise.entity.EntityMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DataOrchestrator {

    private final CacheService cacheService;
    private final DBService dbService;

    public DataOrchestrator(CacheService cacheService, DBService dbService) {
        this.cacheService = cacheService;
        this.dbService = dbService;
    }

    @PostConstruct
    public void warmUpCache() {
        // TODO: подумать чо буду в при старте загружать
    }
    @PreDestroy
    public void onShutdown() {
        // TODO: подумать чо буду при завершении делать
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(FullUserDTO user) {
        dbService.saveUser(EntityMapper.toEntity(user)); // синхронно в бд
        cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
    }
    public void enableUser(long userId) {
        boolean isUpdated = dbService.enableUser(userId) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateUserIsEnabled(userId, true); // сохраняем в кеш
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
        cacheService.updateUserLastLogin(username, lastLogin); // сохраняем в кеш
    }
    public void updateUserProfile(UserProfileDTO profile) {
        boolean isUpdated = dbService.updateUserProfile(profile.getUserId(), profile.getUsername(), profile.getName()) > 0; // синхронно в БД
        if (isUpdated) cacheService.updateUserProfile(profile.getUserId(), profile.getUsername(), profile.getName()); // обновляем в кеше
    }
    public void deleteUser(long userId) {
        boolean isUpdated = dbService.deleteUser(userId) > 0; // синхронно в бд
        if (isUpdated) cacheService.deleteUser(userId); // сохраняем в кеш
    }
    public void restoreUser(long userId) {
        boolean isUpdated = dbService.restoreUser(userId) > 0; // синхронно в бд
        if (isUpdated) cacheService.restoreUser(userId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheService.existsUserByUsername(username))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // восстанавливаем кеш
        });
        return dbUser.isPresent();
    }
    public boolean existsUserByEmail(String email)  {
        // проверяем в кеше
        if (cacheService.existsUserByEmail(email))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByEmail(email);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // восстанавливаем кеш
        });
        return dbUser.isPresent();
    }
    public boolean isActiveUser(long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUser(userId);
        if (cached.isPresent())
            return cached.filter(us -> us.isEnabled() && !us.isDeleted()).isPresent();

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // восстанавливаем кеш
        });
        return dbUser.filter(us -> us.isEnabled() && !us.isDeleted()).isPresent();
    }

    public Optional<FullUserDTO> getUser(long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUser(userId);
        if (cached.isPresent())
            return cached.map(EntityMapper::toFullDTO);

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(EntityMapper::toFullDTO);
    }
    public Optional<FullUserDTO> getUserByUsername(String username) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUserByUsername(username);
        if (cached.isPresent())
            return cached.map(EntityMapper::toFullDTO);

        //грузим из бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(EntityMapper::toFullDTO);
    }
    public Optional<UserProfileDTO> getUserProfile(long userId) {
        // пробуем кеш
        Optional<FullUserDTO> user = getUser(userId);
        if (user.isEmpty())
            return Optional.empty();

        // грузим из бд
        return user.map(EntityMapper::toUserProfileDTO);
    }

    private Map<Long, FullUserDTO> loadUsersWithCache(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, FullUserDTO> userMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingUserIds = new HashSet<>();
        Map<Long, CacheUser> cachedUsers = cacheService.getCacheUsersByIds(userIds, missingUserIds);

        for (Map.Entry<Long, CacheUser> entry : cachedUsers.entrySet()) {
            if (!entry.getValue().isDeleted()) {
                userMap.put(entry.getKey(), EntityMapper.toFullDTO(entry.getValue()));
            }
        }

        // Загружаем недостающих из БД
        if (!missingUserIds.isEmpty()) {
            List<User> dbUsers = dbService.getActiveUserByIds(new ArrayList<>(missingUserIds));
            List<CacheUser> usersToCache = new ArrayList<>();

            for (User user : dbUsers) {
                CacheUser cacheUser = EntityMapper.toCache(user);
                usersToCache.add(cacheUser);
                userMap.put(user.getId(), EntityMapper.toFullDTO(user));
            }

            if (!usersToCache.isEmpty()) {
                cacheService.saveUsers(usersToCache);
            }
        }

        return userMap;
    }
    public UsersPageDTO getUsersPage(String filter, Long cursor, int limit) {
        // получаем пагинацию из бд
        List<UserResult> rows = dbService.getFullFilteredUsersPage(filter, cursor, limit + 1); // берем на одну больше

        Map<Long, LightUserDTO> users = new HashMap<>(rows.size());
        Long nextCursor = null;

        if (!rows.isEmpty()) {
            boolean hasMore = rows.size() > limit;

            List<UserResult> pageRows = hasMore ? rows.subList(0, limit) : rows;

            users = EntityMapper.toLightUserDTOs(pageRows, users);
            nextCursor = hasMore ? pageRows.getLast().getUserId() : null;
        }

        return new UsersPageDTO(users, nextCursor);
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    public void saveLoginHistory(LoginHistoryDTO loginHistory) {
        dbService.saveLoginHistoryAsync(EntityMapper.toEntity(loginHistory)); // асинхронно в бд
    } // TODO: SYNC OUTBOX||KAFKA


    // ========== CHAT METHODS ==========


    // Основные методы
    public void savePersonalChatAndAddPerson(LightChatDTO chat, LightChatMemberDTO creator, LightChatMemberDTO opponent) {
        // синхронно в бд
        dbService.savePersonalChat(EntityMapper.toEntity(chat), opponent.getUserId());

        // сохраняем в кеш
        cacheService.saveChat(EntityMapper.toCache(chat, null));
        cacheService.saveChatMembers(
            chat.getId(), List.of(EntityMapper.toCache(creator), EntityMapper.toCache(opponent))
        );
    }
    public void saveGroupChatAndAddPeople(LightChatDTO chat, List<LightChatMemberDTO> chatMembers) {
        // конвертируем
        Long[] memberIds = new Long[chatMembers.size()];
        Boolean[] isAdminFlags = new Boolean[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            LightChatMemberDTO member = chatMembers.get(i);
            memberIds[i] = member.getUserId();
            isAdminFlags[i] = member.isAdmin();
        }

        // синхронно в бд
        dbService.saveGroupChat(EntityMapper.toEntity(chat), memberIds, isAdminFlags);

        // сохраняем в кеш
        cacheService.saveChat(EntityMapper.toCache(chat, null));
        cacheService.saveChatMembers(chat.getId(), chatMembers.stream().map(EntityMapper::toCache).toList());
    }
    public void restoreChat(long chatId) {
        boolean isUpdated = dbService.restoreChat(chatId) > 0; // синхронно в бд
        if (isUpdated) cacheService.restoreChat(chatId); // сохраняем в кеш
    }
    public void deleteChat(long chatId) {
        boolean isUpdated = dbService.deleteChat(chatId) > 0; // синхронно в бд
        if (isUpdated) cacheService.deleteChat(chatId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public Optional<LightChatDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toLightDTO);

        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(fullChat -> {
            cacheService.saveChat(EntityMapper.toCache(fullChat)); // восстанавливаем в кеш
        });
        return dbFullChat.filter(chat -> !chat.getIsDeleted()).map(EntityMapper::toLightDTO);
    }
    public Optional<LightChatDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(EntityMapper::toLightDTO);

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullPersonalChat(userId1, userId2);
        dbFullChat.ifPresent(fullChat -> {
            cacheService.saveChat(EntityMapper.toCache(fullChat)); // восстанавливаем в кеш
        });
        return dbFullChat.map(EntityMapper::toLightDTO);
    }

    public boolean isActiveChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isActive = cacheService.isActiveChat(chatId);
        if (isActive.isPresent())
            return isActive.get();

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(fullChat -> {
            cacheService.saveChat(EntityMapper.toCache(fullChat)); // восстанавливаем в кеш
        });
        return dbFullChat.filter(chat -> !chat.getIsDeleted()).isPresent();
    }
    public Optional<Boolean> isGroupChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.isActiveGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(fullChat -> {
            cacheService.saveChat(EntityMapper.toCache(fullChat)); // восстанавливаем в кеш
        });
        return dbFullChat.map(FullChatResult::getIsGroup);
    }

    public UserChatsPageDTO getUserChatsPage(long userId, Long cursor, int limit) {
        // загружаем с бд
        List<UserFullChatResult> rows = dbService.getFullUserChatsPage(userId, cursor, limit + 1); // берем на одну больше

        Map<Long, FullChatDTO> chats = new HashMap<>(rows.size());
        boolean hasMore = rows.size() > limit;

        List<UserFullChatResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        chats = EntityMapper.toFullDTOs(pageRows, chats);
        Long nextCursor = hasMore ? pageRows.getLast().getId() : null;

        // кешируем данные
        cacheService.saveChats(EntityMapper.toCacheChats(chats.values()));
        return new UserChatsPageDTO(chats, nextCursor);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveOrRestoreChatMember(LightChatMemberDTO chatMember) {
        dbService.upsertChatMember(EntityMapper.toEntity(chatMember)); // синхронно в бд
        cacheService.saveChatMember(EntityMapper.toCache(chatMember)); // сохраняем в кеш
    }
    public void saveChatMembers(long chatId, List<LightChatMemberDTO> chatMembers) {
        // конвертируем
        Long[] ids = new Long[chatMembers.size()];
        Boolean[] isAdminFlags = new Boolean[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            LightChatMemberDTO member = chatMembers.get(i);
            ids[i] = member.getUserId();
            isAdminFlags[i] = member.isAdmin();
        }

        dbService.upsertChatMembers(chatId, ids, isAdminFlags); // синхронно в бд
        cacheService.saveChatMembers(chatId, EntityMapper.toCacheLightChatMembers(chatMembers)); // сохраняем в кеш
    }
    public void updateAdminRights(long chatId, long userId, boolean isAdmin) {
        boolean isUpdated = dbService.updateUserAdminRights(chatId, userId, isAdmin) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateAdminRights(chatId, userId, isAdmin); // обновляем кэш
    }
    public void removeUserFromChat(long chatId, long userId) {
        boolean removed = dbService.removeUserFromChat(userId, chatId); // синхронно в бд
        if (removed) cacheService.removeChatMember(userId, chatId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean hasActiveChatMember(long chatId, long userId) {
        // проверка в кеше
        Optional<Boolean> hasActiveChatMember = cacheService.hasActiveChatMember(chatId, userId);
        if (hasActiveChatMember.isPresent())
            return hasActiveChatMember.get();

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbService.getChatMember(chatId, userId);
        dbMember.ifPresent(member -> {
            cacheService.saveChatMember(EntityMapper.toCache(member)); // кешируем
        });
        return dbMember.map(ChatMember::isActive).orElse(false);
    }
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        // пробуем кеш
        Optional<Boolean> cached = cacheService.isActiveAdminInActiveChat(chatId, userId);
        if (cached.isPresent())
            return cached;

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbService.getActiveChatMember(chatId, userId);
        dbMember.ifPresent(member -> {
            cacheService.saveChatMember(EntityMapper.toCache(member));
        });
        return dbMember.map(ChatMember::isAdmin);
    }

    private Map<Long, LightChatMemberDTO> loadMembersWithCache(long chatId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, LightChatMemberDTO> memberMap = new HashMap<>();

        // Загружаем из кеша
        Set<Long> missingMemberIds = new HashSet<>();
        Map<Long, CacheChatMember> cachedMembers = cacheService.getChatMembers(chatId, userIds, missingMemberIds);

        for (Map.Entry<Long, CacheChatMember> entry : cachedMembers.entrySet()) {
            CacheChatMember cachedMember = entry.getValue();
            if (!cachedMember.isDeleted()) {
                memberMap.put(entry.getKey(), EntityMapper.toLightDTO(cachedMember));
            }
        }

        // Загружаем недостающих из БД
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbService.getActiveChatMembersByIds(chatId, new ArrayList<>(missingMemberIds));
            List<CacheChatMember> membersToCache = new ArrayList<>();

            for (ChatMember member : dbMembers) {
                CacheChatMember cacheMember = EntityMapper.toCache(member);
                membersToCache.add(cacheMember);
                memberMap.put(member.getUserId(), EntityMapper.toLightDTO(member));
            }

            if (!membersToCache.isEmpty()) {
                cacheService.saveChatMembers(chatId, membersToCache);
            }
        }

        return memberMap;
    }
    public ChatMembersPageDTO getChatMembersPage(long chatId, Long cursor, int limit) {
        List<Long> userIds = dbService.getChatMemberIdsPage(chatId, cursor, limit + 1);
        if (userIds.isEmpty()) {
            return new ChatMembersPageDTO(Collections.emptyMap(), null);
        }

        boolean hasMore = userIds.size() > limit;
        List<Long> resultUserIds = hasMore ? userIds.subList(0, limit) : userIds;
        Long nextCursor = hasMore ? resultUserIds.getLast() : null;

        // Загружаем все необходимые данные
        Map<Long, FullUserDTO> userMap = loadUsersWithCache(new HashSet<>(resultUserIds));
        Map<Long, LightChatMemberDTO> memberMap = loadMembersWithCache(chatId, new HashSet<>(resultUserIds));

        // Формируем результат
        Map<Long, FullChatMemberDTO> result = new LinkedHashMap<>();
        for (Long userId : resultUserIds) {
            FullUserDTO user = userMap.get(userId);
            LightChatMemberDTO member = memberMap.get(userId);
            if(user == null || member == null) continue;

            result.put(userId, EntityMapper.toFullDTO(user, member));
        }

        return new ChatMembersPageDTO(result, nextCursor);
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(VerificationTokenDTO verificationTokenDTO) {
        dbService.saveVerificationTokenAsync(EntityMapper.toEntity(verificationTokenDTO)); // асинхронно в бд
        cacheService.saveVerificationToken(EntityMapper.toCache(verificationTokenDTO)); // сохраняем в кеш
    } // TODO: SYNC OUTBOX||KAFKA
    public void deleteVerificationToken(String token) {
        dbService.deleteVerificationTokenAsync(token); // асинхронно в бд
        cacheService.deleteVerificationToken(token); // сохраняем в кеш
    } // TODO: SYNC OUTBOX||KAFKA


    // Вспомогательные методы
    public Optional<VerificationTokenDTO> getVerificationToken(String token) {
        Optional<CacheVerificationToken> optToken = cacheService.getVerificationToken(token);
        if(optToken.isPresent())
            return optToken.map(EntityMapper::toDTO);

        Optional<VerificationToken> optTokenDB = dbService.getVerificationToken(token);
        optTokenDB.ifPresent(verificationTokenDB -> {
            cacheService.saveVerificationToken(EntityMapper.toCache(verificationTokenDB));
        });
        return optTokenDB.map(EntityMapper::toDTO);
    }


    // ========== MESSAGE METHODS ==========


    // Основные методы
    public void saveMessage(LightMessageDTO message) {
        dbService.saveMessage(EntityMapper.toEntity(message)); // синхронно в бд
        cacheService.saveNewMessage(EntityMapper.toCache(message)); // сохраняем в кеш
    }
    public void markMessageAsRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        dbService.markMessageAsRead(chatId, userId, messageId, readAt); // синхронно в бд
        cacheService.markMessageAsRead(chatId, userId, messageId); // сохраняем в кеш
    }
    public void restoreMessage(long chatId, long messageId) {
        boolean isUpdated = dbService.restoreMessage(messageId) > 0; // синхронно в бд
        if (isUpdated) cacheService.restoreMessage(chatId, messageId); // сохраняем в кеш
    }
    public void deleteMessage(long chatId, long messageId) {
        boolean isUpdated = dbService.deleteMessage(messageId) > 0; // синхронно в бд
        if (isUpdated) cacheService.deleteMessage(chatId, messageId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean isActiveMessageInChat(long chatId, long messageId) {
        // пробуем кеш
        Optional<CacheMessage> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> !msg.isHiddenByAdmin() && msg.getChatId() == chatId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbService.getMessage(messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(msg -> !msg.isHiddenByAdmin() && msg.getChatId() == chatId).isPresent();
    }
    public boolean isActiveMessageInChatAndIsSender(long chatId, long userId, long messageId) {
        // пробуем кеш
        Optional<CacheMessage> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> !msg.isHiddenByAdmin() && msg.getChatId() == chatId && msg.getSenderId() == userId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbService.getMessage(messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(msg -> !msg.isHiddenByAdmin() && msg.getChatId() == chatId && msg.getSenderId() == userId).isPresent();
    }

    public Optional<LightMessageDTO> getActiveMessageWithReadStatusInChat(long chatId, long userId, long messageId) {
        // пробуем кеш
        Optional<CacheMessage> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent()){
            return cacheMessage.map(msg -> {
                if (msg.isHiddenByAdmin()) msg.setText(null);
                if (msg.getChatId() != chatId) return null;

                // Получаем статус прочтения
                Long lastReadId = getUserLastRead(chatId, userId);
                boolean isRead = lastReadId != -1 && lastReadId >= messageId;
                return EntityMapper.toLightDTO(msg, isRead);
            });
        }

        // грузим из бд
        Optional<UserMessageDBResult> dbMessage = dbService.getMessageWithReadStatus(userId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.map(msg -> {
            if (msg.getChatId() != chatId) return null;

            LightMessageDTO newMsg = EntityMapper.toLightDTO(msg);
            if (newMsg.isHiddenByAdmin()) newMsg.setText(null);
            return newMsg;
        });
    }
    public MessagesPageDTO getChatMessagesPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        // Получаем IDs сообщений из БД
        List<Long> messageIds = dbService.getMessageIdsPage(chatId, cursor, limit + 1, direction); // Получаем с БД
        if (messageIds.isEmpty()) {
            return new MessagesPageDTO(Collections.emptyMap(), null);
        }

        // Загружаем сообщения из кеша
        Set<Long> missingIds = new HashSet<>();
        Map<Long, CacheMessage> cachedMessages = cacheService.getMessages(messageIds, missingIds); // Получаем с КЕША

        // Загружаем недостающие сообщения из БД
        if (!missingIds.isEmpty()) {
            List<Message> dbMessages = dbService.getMessagesByIds(chatId, missingIds); // Получаем с БД
            List<CacheMessage> toCache = new ArrayList<>();

            for (Message dbMsg : dbMessages) {
                toCache.add(EntityMapper.toCache(dbMsg));
                cachedMessages.put(dbMsg.getId(), EntityMapper.toCache(dbMsg));
            }

            cacheService.saveMessages(toCache); // Кешируем
        }

        // Получаем статус прочтения
        Long lastReadId = getUserLastRead(chatId, userId);

        // Собираем результат в правильном порядке (как из БД)
        List<LightMessageDTO> messages = new ArrayList<>();
        for (Long id : messageIds) {
            CacheMessage msg = cachedMessages.get(id);
            if (msg != null) {
                if (msg.isHiddenByAdmin()) msg.setText(null);

                boolean isRead = lastReadId != -1 && lastReadId >= msg.getId();
                messages.add(EntityMapper.toLightDTO(msg, isRead));
            }
        }

        Long nextCursor = null;
        if (messages.size() > limit) {
            if (direction == Direction.FORWARD) {
                messages = messages.subList(0, limit);
                nextCursor = messages.getLast().getId();
            } else {
                messages = messages.subList(messages.size() - limit, messages.size());
                nextCursor = messages.getFirst().getId();
            }
        }

        Map<Long, LightMessageDTO> messageMap = new LinkedHashMap<>();
        for (LightMessageDTO message : messages) {
            messageMap.put(message.getId(), message);
        }

        return new MessagesPageDTO(messageMap, nextCursor);
    }
    public ChatStatsDBResult getChatClearStats(long chatId, long userId) {
        return dbService.getChatMessagesDeletedStats(chatId, userId);
    }

    public Long getUserLastRead(long chatId, long userId){
        Long lastReadId = cacheService.getUserLastRead(chatId, userId); // Получаем с КЕША
        if (lastReadId == null) {
            Optional<Long> userLastReadMessageId = dbService.getUserReadStatusByChatId(chatId, userId); // Получаем с БД
            lastReadId = userLastReadMessageId.orElse(-1L);
            cacheService.updateLastReadByUser(chatId, userId, lastReadId); // Кешируем
        }
        return lastReadId;
    }


    // ========== SUB METHODS ==========


    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }
    @Scheduled(initialDelay = 10_000, fixedRate = 86_400_000) // Каждые 24 часа
    public void cleanupExpiredTokens() {
        try {
            int numDeletedTokens = dbService.cleanupExpiredVerificationTokens();
            log.info("[🔧] ✅ Expired tokens cleanup completed. Deleted --> {} tokens", numDeletedTokens);
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during token cleanup: {}", e.getMessage());
        }
    }
}