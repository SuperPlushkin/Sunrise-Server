package com.sunrise.core.dataservice;

import com.sunrise.core.dataservice.type.*;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.entity.cache.*;
import com.sunrise.entity.db.*;
import com.sunrise.entity.dto.*;
import com.sunrise.entity.EntityMapper;

import com.sunrise.entity.pagination.ChatMembersPageDTO;
import com.sunrise.entity.pagination.MessagesPageDTO;
import com.sunrise.entity.pagination.UserChatsPageDTO;
import com.sunrise.entity.pagination.UsersPageDTO;
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
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
        cacheService.updateUserLastLogin(username, lastLogin); // сохраняем в кеш
    }
    public void updateUserProfile(long userId, String username, String name, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateUserProfile(userId, username, name, updatedAt) > 0; // синхронно в БД
        if (isUpdated) cacheService.updateUserProfile(userId, username, name, updatedAt); // обновляем в кеше
    }
    public void updateUserEmail(long userId, String email, LocalDateTime updatedAt) {
        int newVersion = dbService.updateUserEmailAndGetJwtVersion(userId, email, updatedAt);
        cacheService.updateUserEmailAndJwtVersion(userId, email, newVersion, updatedAt);
    }
    public void updateUserPassword(long userId, String password, LocalDateTime updatedAt) {
        int newVersion = dbService.updateUserPasswordAndGetJwtVersion(userId, password, updatedAt);
        cacheService.updateUserPasswordAndJwtVersion(userId, password, newVersion, updatedAt);
    }
    public void enableUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.enableUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.enableUser(userId, updatedAt); // сохраняем в кеш
    }
    public void disableUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.disableUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.disableUser(userId, updatedAt); // сохраняем в кеш
    }
    public void deleteUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.deleteUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.deleteUser(userId, updatedAt); // сохраняем в кеш
    }
    public void restoreUser(long userId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.restoreUser(userId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.restoreUser(userId, updatedAt); // сохраняем в кеш
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
    public Optional<Integer> getUserJwtVersion(long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUser(userId);
        if (cached.isPresent())
            return cached.map(CacheUser::getJwtVersion);

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // восстанавливаем кеш
        });
        return dbUser.map(User::getJwtVersion);
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
    public UsersPageDTO getActiveUsersPage(String filter, Long cursor, int limit) {
        // получаем пагинацию из бд
        List<UserResult> rows = dbService.getActiveUsersPage(filter, cursor, limit + 1); // берем на одну больше

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
    public void savePersonalChatAndAddMembers(LightChatDTO chat, ChatMemberDTO creator, ChatMemberDTO opponent) {
        // синхронно в бд
        dbService.savePersonalChat(EntityMapper.toEntity(chat), opponent.getUserId());

        // сохраняем в кеш
        cacheService.saveChatAndAddMembers(
            EntityMapper.toCache(chat),
            List.of(EntityMapper.toCache(creator), EntityMapper.toCache(opponent))
        );
    }
    public void saveGroupChatAndAddMembers(LightChatDTO chat, List<ChatMemberDTO> chatMembers) {
        // конвертируем
        Long[] memberIds = new Long[chatMembers.size()];
        Boolean[] isAdminFlags = new Boolean[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            ChatMemberDTO member = chatMembers.get(i);
            memberIds[i] = member.getUserId();
            isAdminFlags[i] = member.isAdmin();
        }

        // синхронно в бд
        dbService.saveGroupChat(EntityMapper.toEntity(chat), memberIds, isAdminFlags);

        // сохраняем в кеш
        cacheService.saveChatAndAddMembers(
            EntityMapper.toCache(chat),
            chatMembers.stream().map(EntityMapper::toCache).toList()
        );
    }
    public void updateChatInfo(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatInfo(chatId, newName, newDescription, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateChatInfo(chatId, newName, newDescription, updatedAt); // сохраняем в кеш
    }
    public void updateChatType(long chatId, ChatType newType, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatType(chatId, newType, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateChatType(chatId, newType, updatedAt); // сохраняем в кеш
    }
    public void restoreChat(long chatId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.restoreChat(chatId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.restoreChat(chatId, updatedAt); // сохраняем в кеш
    }
    public void deleteChat(long chatId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.deleteChat(chatId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.deleteChat(chatId, updatedAt); // сохраняем в кеш
    }


    // Вспомогательные методы
    public Optional<LightChatDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toLightDTO);

        Optional<Chat> dbChat = dbService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.filter(chat -> !chat.isDeleted()).map(EntityMapper::toLightDTO);
    }
    public Optional<LightChatDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(EntityMapper::toLightDTO);

        // грузим из бд
        Optional<Chat> dbChat = dbService.getPersonalChat(userId1, userId2);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.map(EntityMapper::toLightDTO);
    }

    public boolean isActiveChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isActive = cacheService.isActiveChat(chatId);
        if (isActive.isPresent())
            return isActive.get();

        // грузим из бд
        Optional<Chat> dbChat = dbService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.filter(Chat::isActive).isPresent();
    }
    public Optional<Boolean> isGroupChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.isActiveGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<Chat> dbChat = dbService.getChat(chatId);
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toCache(chat)); // восстанавливаем в кеш
        });
        return dbChat.map(Chat::isNotPersonal);
    }

    public UserChatsPageDTO getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, Long chatIdCursor, int limit) {
        // загружаем с бд
        List<UserChatResult> rows = dbService.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit + 1); // берем на одну больше
        if (rows.isEmpty()) {
            return new UserChatsPageDTO(Collections.emptyMap(), null);
        }

        Map<Long, FullChatDTO> chats = new HashMap<>(rows.size());
        boolean hasMore = rows.size() > limit;

        List<UserChatResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        chats = EntityMapper.toFullDTOs(pageRows, chats);
        Long nextCursor = hasMore ? pageRows.getLast().getId() : null;

        // кешируем данные
        cacheService.saveChats(EntityMapper.toCacheChats(chats.values()));
        return new UserChatsPageDTO(chats, nextCursor);
    }
    public Optional<FullChatDTO> getUserChat(long chatId, long userId) {
        // загружаем с бд
        Optional<UserChatResult> dbChat = dbService.getUserChat(chatId, userId);

        // кешируем данные
        dbChat.ifPresent(chat -> {
            cacheService.saveChat(EntityMapper.toCache(chat));
        });
        return dbChat.map(EntityMapper::toFullDTO);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveOrRestoreChatMember(ChatMemberDTO chatMember) {
        dbService.upsertChatMember(EntityMapper.toEntity(chatMember)); // синхронно в бд
        cacheService.saveChatMember(EntityMapper.toCache(chatMember)); // сохраняем в кеш
    }
    public void saveOrRestoreChatMembers(long chatId, List<ChatMemberDTO> chatMembers) {
        // конвертируем
        LocalDateTime joinedAt = chatMembers.getFirst().getJoinedAt();
        Long[] ids = new Long[chatMembers.size()];
        Boolean[] isAdminFlags = new Boolean[chatMembers.size()];
        for (int i = 0; i < chatMembers.size(); i++) {
            ChatMemberDTO member = chatMembers.get(i);
            ids[i] = member.getUserId();
            isAdminFlags[i] = member.isAdmin();
        }

        dbService.upsertChatMembers(chatId, ids, joinedAt, isAdminFlags); // синхронно в бд
        cacheService.saveChatMembers(chatId, EntityMapper.toCacheLightChatMembers(chatMembers)); // сохраняем в кеш
    }
    public void updateChatMemberInfo(long chatId, long userId, String tag, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatMemberInfo(chatId, userId, tag, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateChatMemberInfo(chatId, userId, tag, updatedAt); // обновляем кэш
    }
    public void updateChatMemberAdminRights(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatMemberAdminRights(chatId, userId, isAdmin, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateChatMemberAdminRights(chatId, userId, isAdmin, updatedAt); // обновляем кэш
    }
    public void updateChatMemberSetting(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateChatMemberSettings(chatId, userId, isPinned, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.updateChatMemberSettings(chatId, userId, isPinned, updatedAt); // обновляем кэш
    }
    public void removeUserFromChat(long chatId, long userId, LocalDateTime updatedAt) {
        boolean removed = dbService.removeUserFromChat(userId, chatId, updatedAt); // синхронно в бд
        if (removed) cacheService.removeChatMember(userId, chatId, updatedAt); // сохраняем в кеш
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

    private Map<Long, ChatMemberDTO> loadMembersWithCache(long chatId, Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, ChatMemberDTO> memberMap = new HashMap<>();

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
        Map<Long, ChatMemberDTO> memberMap = loadMembersWithCache(chatId, new HashSet<>(resultUserIds));

        // Формируем результат
        Map<Long, ChatMemberProfileDTO> result = new LinkedHashMap<>();
        for (Long userId : resultUserIds) {
            FullUserDTO user = userMap.get(userId);
            ChatMemberDTO member = memberMap.get(userId);
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
    public void saveMessage(MessageDTO message) {
        dbService.saveMessage(EntityMapper.toEntity(message)); // синхронно в бд
        cacheService.saveMessage(EntityMapper.toCache(message)); // сохраняем в кеш
    }
    public void updateMessage(long messageId, String newText, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.updateMessage(messageId, newText, updatedAt) > 0; // синхронно в бд
    }
    public void markMessagesUpToRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        dbService.markMessagesUpToRead(chatId, userId, messageId, readAt); // синхронно в бд
    }
    public void restoreMessage(long messageId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.restoreMessage(messageId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.restoreMessage(messageId); // сохраняем в кеш
    }
    public void deleteMessage(long messageId, LocalDateTime updatedAt) {
        boolean isUpdated = dbService.deleteMessage(messageId, updatedAt) > 0; // синхронно в бд
        if (isUpdated) cacheService.deleteMessage(messageId, updatedAt); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean isActiveMessageInChat(long chatId, long messageId) {
        // пробуем кеш
        Optional<CacheMessage> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbService.getMessage(messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId).isPresent();
    }
    public boolean isActiveMessageInChatAndIsSender(long chatId, long userId, long messageId) {
        // пробуем кеш
        Optional<CacheMessage> cacheMessage = cacheService.getMessage(messageId);
        if (cacheMessage.isPresent())
            return cacheMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId && msg.getSenderId() == userId).isPresent();

        // грузим из бд
        Optional<Message> dbMessage = dbService.getMessage(messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.filter(msg -> msg.isActive() && msg.getChatId() == chatId && msg.getSenderId() == userId).isPresent();
    }

    public Optional<MessageDTO> getActiveMessageWithReadStatusInChat(long chatId, long userId, long messageId) {
        // грузим из бд
        Optional<UserMessageDBResult> dbMessage = dbService.getMessageWithReadStatus(userId, messageId);
        dbMessage.ifPresent(msg -> {
            cacheService.saveMessage(EntityMapper.toCache(msg)); // восстанавливаем в кеш
        });
        return dbMessage.map(msg -> {
            if (msg.getChatId() != chatId) return null;

            MessageDTO newMsg = EntityMapper.toLightDTO(msg);
            if (newMsg.isDeleted()) newMsg.setText(null);
            return newMsg;
        });
    }
    public MessagesPageDTO getChatMessagesPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        // Получаем Page сообщений из БД
        List<UserMessageDBResult> dbResult = dbService.getMessagePage(chatId, userId, cursor, limit + 1, direction); // Получаем с БД
        if (dbResult.isEmpty()) {
            return new MessagesPageDTO(Collections.emptyMap(), null);
        }

        // обрезаем и выясняем курсор (если требуется)
        Long nextCursor = null;
        if (dbResult.size() > limit) {
            if (direction == Direction.FORWARD) {
                dbResult = dbResult.subList(0, limit);
                nextCursor = dbResult.getLast().getId();
            } else {
                dbResult = dbResult.subList(dbResult.size() - limit, dbResult.size());
                nextCursor = dbResult.getFirst().getId();
            }
        }

        // собираем результат
        Map<Long, MessageDTO> messageMap = new LinkedHashMap<>(dbResult.size());
        List<CacheMessage> messagesToCache = new ArrayList<>(dbResult.size());
        for (UserMessageDBResult message : dbResult) {
            log.debug("msg -> {}", message.toString());
            MessageDTO msgDTO = EntityMapper.toLightDTO(message);
            if (msgDTO.getChatId() != chatId) continue;
            if (msgDTO.isDeleted()) msgDTO.setText(null);

            messageMap.put(message.getId(), msgDTO);
            messagesToCache.add(EntityMapper.toCache(msgDTO));
        }

        // кешируем
        cacheService.saveMessages(messagesToCache);
        return new MessagesPageDTO(messageMap, nextCursor);
    }
    public Map<Long, MessageReadStatusDTO> getMessageReads(long messageId){
        List<MessageReadStatusResult> reads = dbService.getMessageReaders(messageId);
        return EntityMapper.toMessageReadDTOs(reads, new HashMap<>(reads.size()));
    }

    public ChatStatsDBResult getChatClearStats(long chatId, long userId) {
        return dbService.getChatMessagesDeletedStats(chatId, userId);
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