package com.Sunrise.Core.DataServices;

import com.Sunrise.DTOs.DBResults.*;
import com.Sunrise.DTOs.Paginations.*;
import com.Sunrise.Entities.Caches.*;
import com.Sunrise.Entities.DBs.*;
import com.Sunrise.Entities.DTOs.*;
import com.Sunrise.Entities.EntityMapper;

import com.Sunrise.Subclasses.SimpleSnowflakeId;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataOrchestrator {

    private final CacheService cacheService;
    private final DBService dbService;

    public DataOrchestrator(CacheService cacheService, DBService dbService) {
        this.cacheService = cacheService;
        this.dbService = dbService;
    }


    // ========== CACHE METHODS ==========


    @PostConstruct
    public void warmUpCache() {
        // TODO: подумать чо буду в при старте загружать
    }
    @PreDestroy
    public void onShutdown() {
        // TODO: подумать чо буду при завершении делать
    }


    // Основные методы
    private void cacheUser(User user) {
        cacheService.saveUser(EntityMapper.toCache(user));
        log.debug("[⚡] Cached user {} || cacheUser", user.getId());
    }
    private void cacheUser(ChatOpponentResult opponent) {
        cacheService.saveUser(EntityMapper.toCache(opponent));
        log.debug("[⚡] Cached opponent {} || cacheUser", opponent.getUserId());
    }
    private void cacheUserChat(FullChatResult fullChat) {
        cacheService.saveChat(EntityMapper.toCache(fullChat));
        log.debug("[⚡] Cached full chat {} with container ({} messages) || cacheChat", fullChat.getId(), fullChat.getMessagesCount());
    }
    private CacheChat cacheUserChat(UserFullChatResult userFullChat) {
        CacheChat cacheChat = EntityMapper.toCache(userFullChat);
        cacheService.saveChat(cacheChat);
        log.debug("[⚡] Cached full user chat {} with container ({} messages) || cacheChat", cacheChat.getId(), userFullChat.getMessagesCount());
        return cacheChat;
    }
    private void cacheChatMember(ChatMember member) {
        cacheService.addChatMember(EntityMapper.toCache(member));
        log.debug("[⚡] Cached chat member {} in chat {} || cacheChatMember", member.getUserId(), member.getChatId());
    }
    private void cacheMessages(CacheChat cacheChat, List<UserMessageDBResult> dbResults) {
        if (dbResults == null || dbResults.isEmpty()) return;

        cacheChat.addMessages(EntityMapper.toCacheMessages(dbResults));
        log.debug("[⚡] Cached {} messages in chat {} || cacheMessages", dbResults.size(), cacheChat.getId());
    }


    // Вспомогательные методы
    private Optional<CacheChat> getOrLoadCacheChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getCacheChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat;

        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(this::cacheUserChat);
        return dbFullChat.map(EntityMapper::toCache);
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(FullUserDTO user) {
        dbService.saveUser(EntityMapper.toEntity(user)); // синхронно в бд
        cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
    }
    public void updateUser(FullUserDTO user) {
        dbService.saveUser(EntityMapper.toEntity(user)); // синхронно в бд
        cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
    }
    public void enableUser(long userId) {
        dbService.enableUser(userId); // синхронно в бд
        cacheService.updateUserIsEnabled(userId, true); // сохраняем в кеш
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
        cacheService.updateUserLastLogin(username, lastLogin); // сохраняем в кеш
    }
    public void updateUserProfile(UserProfileDTO profile) {
        dbService.updateUserProfile(profile.getUserId(), profile.getUsername(), profile.getName()); // синхронно в БД
        cacheService.updateUserProfile(profile.getUserId(), profile.getUsername(), profile.getName()); // обновляем в кеше
    }
    public void deleteUser(long userId) {
        dbService.deleteUser(userId); // синхронно в бд
        cacheService.deleteUser(userId); // сохраняем в кеш
    }
    public void restoreUser(long userId) {
        dbService.restoreUser(userId); // синхронно в бд
        cacheService.restoreUser(userId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public Optional<FullUserDTO> getUser(long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getCacheUser(userId);
        if (cached.isPresent())
            return cached.map(EntityMapper::toFullDTO);

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        dbUser.ifPresent(this::cacheUser);
        return dbUser.map(EntityMapper::toFullDTO);
    }
    public Optional<FullUserDTO> getUserByUsername(String username) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUserByUsername(username);
        if (cached.isPresent())
            return cached.map(EntityMapper::toFullDTO);

        //грузим из бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        dbUser.ifPresent(this::cacheUser);
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

    public UsersPageDTO getUsersPage(String filter, Long cursor, int limit) {
        // получаем пагинацию из бд
        List<UserResult> rows = dbService.getFullFilteredUsersPage(filter, cursor, limit + 1); // берем на одну больше
        log.debug("[🏛️] Got users page filter='{}', cursor={}, limit={} from DB", filter, cursor, limit);

        Map<Long, LightUserDTO> users = new HashMap<>(rows.size());
        Long nextCursor = null;

        UsersPagination dbPagination = new UsersPagination(SimpleSnowflakeId.nextId(), filter, cursor, limit);
        if (!rows.isEmpty()) {
            boolean hasMore = rows.size() > limit;

            List<UserResult> pageRows = hasMore ? rows.subList(0, limit) : rows;

            users = EntityMapper.toLightDTOs(pageRows, users);
            nextCursor = hasMore ? pageRows.getLast().getUserId() : null;

            dbPagination.setPaginationData(users.keySet(), nextCursor);
        }

        return new UsersPageDTO(users, nextCursor);
    }

    public boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheService.existsUserByUsername(username))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        dbUser.ifPresent(this::cacheUser);
        return dbUser.isPresent();
    }
    public boolean existsUserByEmail(String email)  {
        // проверяем в кеше
        if (cacheService.existsUserByEmail(email))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByEmail(email);
        dbUser.ifPresent(this::cacheUser);
        return dbUser.isPresent();
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
        cacheService.addNewChatMembers(
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
        cacheService.addNewChatMembers(chat.getId(), chatMembers.stream().map(EntityMapper::toCache).toList());
    }
    public void restoreChat(long chatId) {
        // Получаем всех участников чата до восстановления (из бд)
        List<Long> membersIds = dbService.getChatMemberIds(chatId);

        dbService.restoreChat(chatId); // синхронно в бд
        cacheService.restoreChat(chatId, membersIds); // сохраняем в кеш
    }
    public void deleteChat(long chatId) {
        // Получаем всех участников чата до удаления (из бд)
        List<Long> membersIds = dbService.getChatMemberIds(chatId);

        dbService.deleteChat(chatId); // синхронно в бд
        cacheService.deleteChat(chatId, membersIds); // сохраняем в кеш
    }


    // Вспомогательные методы
    public Optional<LightChatDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getCacheChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toLightDTO);

        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(this::cacheUserChat);
        return dbFullChat.map(EntityMapper::toLightDTO).filter(LightChatDTO::isActive);
    }
    public Optional<LightChatDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(EntityMapper::toLightDTO);

        // грузим из бд
        Optional<FullChatResult> dbChat = dbService.getFullPersonalChat(userId1, userId2);
        dbChat.ifPresent(this::cacheUserChat);
        return dbChat.map(EntityMapper::toLightDTO);
    }

    public boolean isActiveChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isActive = cacheService.isActiveChat(chatId);
        if (isActive.isPresent())
            return isActive.get();

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(this::cacheUserChat); // восстанавливаем в кеш
        return dbFullChat.map(fullChatResult -> !fullChatResult.getIsDeleted()).orElse(false);
    }
    public Optional<Boolean> isGroupChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.isActiveGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        dbFullChat.ifPresent(this::cacheUserChat); // восстанавливаем кеш
        return dbFullChat.map(FullChatResult::getIsGroup);
    }
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        // пробуем кеш
        Optional<Boolean> cached = cacheService.isActiveAdminInActiveChat(chatId, userId);
        if (cached.isPresent())
            return cached;

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbService.getActiveChatMember(chatId, userId);
        dbMember.ifPresent(member -> {
            cacheService.addChatMember(EntityMapper.toCache(member));
        });
        return dbMember.map(ChatMember::isAdmin);
    }

    public UserChatsPageDTO getUserChatsPage(long userId, Long cursor, int limit) {
        // пробуем кеш
        Optional<UserChatsPagination> cachedPagination = cacheService.findUserChatsPagination(userId, cursor, limit);
        if (cachedPagination.isPresent()) {
            Optional<UserChatsPageDTO> result = getUserChatsBatch(cachedPagination.get());
            if (result.isPresent()) {
                log.debug("[⚡] Cache hit for user {} chats page cursor={}, limit={}", userId, cursor, limit);
                return result.get();
            } else {
                log.debug("[⚡] Cache invalid for user {} chats page cursor={}, limit={}, loading from DB", userId, cursor, limit);
                cacheService.invalidateUserChatsPagination(userId);
            }
        }

        // загружаем с бд
        List<UserFullChatResult> rows = dbService.getFullUserChatsPage(userId, cursor, limit + 1); // берем на одну больше
        log.debug("[🏛️] Got user {} chats page cursor={}, limit={} from DB", userId, cursor, limit);

        UserChatsPagination dbPagination = new UserChatsPagination(SimpleSnowflakeId.nextId(), userId, cursor, limit);

        Map<Long, FullChatDTO> chats = new HashMap<>(rows.size());
        boolean hasMore = rows.size() > limit;

        List<UserFullChatResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        chats = EntityMapper.toFullDTOs(pageRows, chats);
        Long nextCursor = hasMore ? pageRows.getLast().getId() : null;

        dbPagination.setPaginationData(chats.keySet(), nextCursor);

        // кешируем данные
        cacheService.saveChats(EntityMapper.toCacheChats(chats.values()));
        log.debug("[⚡] Cached {} chats in cache || getUserChatsPage", chats.size());

        // кешируем пагинацию
        cacheService.saveUserChatsPagination(dbPagination);
        log.debug("[⚡] Cached user {} chats page cursor={}, limit={} from DB", userId, cursor, limit);
        return new UserChatsPageDTO(chats, nextCursor);
    }
    private Optional<UserChatsPageDTO> getUserChatsBatch(UserChatsPagination pagination) {
        Set<Long> chatIds = pagination.getItemIds();
        if (chatIds == null || chatIds.isEmpty()) {
            return Optional.of(new UserChatsPageDTO(Collections.emptyMap(), pagination.getNextCursor()));
        }

        long userId = pagination.getUserId();

        Set<Long> missingChatIds = new HashSet<>(chatIds.size() / 2);
        Map<Long, Long> missingChatUserIds = new HashMap<>(chatIds.size() / 2);
        Set<Long> missingUserReadStatusIds = new HashSet<>(chatIds.size() / 2);

        Map<Long, LightChatDTO> chatMap = new HashMap<>(chatIds.size());
        Map<Long, LightUserDTO> userMap = new HashMap<>(chatIds.size());
        Map<Long, LightMessageDTO> messageMap = new HashMap<>(chatIds.size());

        // пытаемся получить чаты (и юзеров для приватных чатов) из Кэша
        List<CacheChat> cacheChats = cacheService.getCacheChats(chatIds, missingChatIds);
        Map<Long, CacheUser> cacheUsers = cacheService.getCacheOpponentsForChats(userId, cacheChats, missingChatUserIds);

        missingChatIds.forEach(chtId -> missingChatUserIds.put());
        missingChatUserIds.put()

        for (Long chatId : chatIds) {
            Optional<CacheChat> cachedChat = cacheService.getCacheChat(chatId);
            if (cachedChat.isPresent()) {
                CacheChat chat = cachedChat.get();
                if (chat.isDeleted()) return Optional.empty();
                if (!chat.isGroup()) {
                    // ищем юзера для приватных чатов или иначе добавляем в мапу для поиска в бд
                    long opponentId = chat.getCreatedBy() == userId ? chat.getOpponentId() : chat.getCreatedBy();
                    Optional<CacheUser> cacheUser = cacheService.getCacheUser(opponentId);
                    if (cacheUser.isPresent()){
                        userMap.put(chatId, EntityMapper.toLightDTO(cacheUser.get()));
                    } else{
                        missingChatUserIds.put(chatId, opponentId);
                    }
                }

                // добавляю чат в мапу
                chatMap.put(chatId, EntityMapper.toLightDTO(chat));

                // добавляю первое сообщение из этого же чата
                chat.getFirstMessage().ifPresent(message -> {
                    chat.isReadByUserOptional(message.getId(), userId).ifPresentOrElse(
                        isReadByUser -> messageMap.put(chatId, EntityMapper.toLightDTO(message, isReadByUser)),
                        () -> {
                            messageMap.put(chatId, EntityMapper.toLightDTO(message, false));
                            missingUserReadStatusIds.add(chatId);
                        }
                    );
                });
            }
            else {
                missingChatIds.add(chatId);
            }
        }

        // загружаем недостающие чаты из БД (и пытаемся получить юзеров для приватных чатов из Кеша)
        if (!missingChatIds.isEmpty()) {
            List<UserFullChatResult> dbChats = dbService.getUserFullChatsByChatIds(missingChatIds, userId);
            for (UserFullChatResult fullChat : dbChats) {
                long chatId = fullChat.getId();
                if (fullChat.getIsDeleted()) continue;
                if (!fullChat.getIsGroup()) {
                    // ищем юзера для приватных чатов или иначе добавляем в мапу для поиска в бд
                    long opponentId = fullChat.getCreatedBy().equals(userId) ? fullChat.getOpponentId() : fullChat.getCreatedBy();
                    Optional<CacheUser> cacheUser = cacheService.getCacheUser(opponentId);
                    if (cacheUser.isPresent()){
                        userMap.put(chatId, EntityMapper.toLightDTO(cacheUser.get()));
                    } else{
                        missingChatUserIds.put(chatId, opponentId);
                    }
                }

                UserMessageDBResult message = fullChat.getLastMessage();

                chatMap.put(chatId, EntityMapper.toLightDTO(fullChat));
                messageMap.put(chatId, EntityMapper.toLightDTO(message));

                // Кэшируем
                CacheChat chat = cacheUserChat(fullChat);
                if (message != null && message.getIsReadByUser()){
                    chat.updateLastReadByUser(userId, message.getId());
                }
            }
        }

        // загружаем недостающих юзеров для личных чатов из БД
        if (!missingChatUserIds.isEmpty()) {
            List<ChatOpponentResult> rows = dbService.findOpponentsForChats(missingChatUserIds);
            rows.forEach(opponent -> {
                cacheUser(opponent);
                userMap.put(opponent.getChatId(), EntityMapper.toCensoredLightUserDTO(opponent));
            });
        }

        // загружаем недостающие последние прочтения сообщений из БД
        if (!missingUserReadStatusIds.isEmpty()) {
            // Загружаем последние сообщения для всех чатов одним запросом
            List<LastUserReadStatusResult> dbLastReadMessages = dbService.getUserReadStatusByChatIds(
                userId, missingUserReadStatusIds
            );

            // Для каждого чата, где нет было последнего прочтения сообщения, проставляем его
            for (LastUserReadStatusResult result : dbLastReadMessages) {
                messageMap.computeIfPresent(result.getChatId(), (chatId, message) -> {
                    message.setReadByUser(message.getId() < result.getLastReadMessageId());
                    return message;
                });
                cacheService.updateLastReadByUser(result.getChatId(), userId, result.getLastReadMessageId());
            }
        }

        // сохраняем порядок вставки и собираем в один класс
        Map<Long, FullChatDTO> finalChatMap = chatMap.keySet().stream()
                .collect(Collectors.toMap(
                    chatId -> chatId,
                    chatId -> {
                        LightChatDTO chat = chatMap.get(chatId);
                        LightMessageDTO message = messageMap.get(chatId);
                        if (chat.isGroup()){
                            return EntityMapper.toFullGroupChatDTO(chat, message);
                        } else {
                            LightUserDTO user = userMap.get(chatId);
                            return EntityMapper.toFullPersonalChatDTO(chat, user, userId, message);
                        }
                    },
                    (a, b) -> b, LinkedHashMap::new)
                );

        return Optional.of(new UserChatsPageDTO(finalChatMap, pagination.getNextCursor()));
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveChatMember(LightChatMemberDTO chatMember) {
        dbService.upsertChatMember(EntityMapper.toEntity(chatMember)); // синхронно в бд
        cacheService.addNewChatMember(EntityMapper.toCache(chatMember)); // сохраняем в кеш
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
        cacheService.addNewChatMembers(chatId, EntityMapper.toCacheLightChatMembers(chatMembers)); // сохраняем в кеш
    }
    public void updateAdminRights(long chatId, long userId, boolean isAdmin) {
        dbService.updateUserAdminRights(chatId, userId, isAdmin); // синхронно в бд
        cacheService.saveOrUpdateAdminRights(chatId, userId, isAdmin); // обновляем кэш
    }
    public void removeUserFromChat(long chatId, long userId) {
        dbService.removeUserFromChat(userId, chatId); // синхронно в бд
        cacheService.removeChatMember(userId, chatId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public boolean hasActiveChatMember(long chatId, long userId) {
        // проверка в кеше
        Optional<Boolean> hasActiveChatMember = cacheService.hasActiveChatMember(chatId, userId);
        if (hasActiveChatMember.isPresent())
            return hasActiveChatMember.get();

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbService.getChatMember(chatId, userId);
        dbMember.ifPresent(this::cacheChatMember); // кешируем
        return dbMember.map(ChatMember::isActive).orElse(false);
    }

    public ChatMembersPageDTO getChatMembersPage(long chatId, Long cursor, int limit) {
        // пробуем кеш
        Optional<ChatMembersPagination> cachedPagination = cacheService.findChatMembersPagination(chatId, cursor, limit);
        if (cachedPagination.isPresent()) {
            Optional<ChatMembersPageDTO> members = getChatMembersBatch(cachedPagination.get());
            if (members.isPresent()){
                log.debug("[⚡] Cache hit for chat {} members page nextCursor={}, limit={}", chatId, cursor, limit);
                return members.get();
            } else {
                cacheService.invalidateChatMembersPagination(chatId);
            }
        }

        // загружаем с бд и сохраняем пагинацию
        List<ChatMemberResult> rows = dbService.getFullChatMembersPage(chatId, cursor, limit + 1); // берем на одну больше
        log.debug("[🏛️] Cached chat {} members page nextCursor={}, limit={} from DB || getChatMembersPage", chatId, cursor, limit);

        ChatMembersPagination dbPagination = new ChatMembersPagination(SimpleSnowflakeId.nextId(), chatId, cursor, limit);

        Map<Long, FullChatMemberDTO> members = new HashMap<>(rows.size());
        boolean hasMore = rows.size() > limit;

        List<ChatMemberResult> pageRows = hasMore ? rows.subList(0, limit) : rows;
        members = EntityMapper.toFullDTOs(pageRows, chatId, members);
        Long nextCursor = hasMore ? pageRows.getLast().getUserId() : null;

        dbPagination.setPaginationData(members.keySet(), nextCursor);

        // кешируем данные
        cacheService.addChatMembers(
            chatId, EntityMapper.toCacheFullChatMembers(members.values())
        );
        log.debug("[⚡] Cached {} chat members  in chat {} || getChatMembersPage", members.size(), chatId);

        // кешируем пагинацию
        cacheService.saveChatMembersPagination(dbPagination);
        log.debug("[⚡] Cached chat {} members page nextCursor={}, limit={} from DB || getChatMembersPage", chatId, cursor, limit);
        return new ChatMembersPageDTO(members, nextCursor);
    }
    private Optional<ChatMembersPageDTO> getChatMembersBatch(ChatMembersPagination pagination) {
        Set<Long> userIds = pagination.getItemIds();
        if (userIds == null || userIds.isEmpty()) {
            return Optional.of(new ChatMembersPageDTO(Collections.emptyMap(), pagination.getNextCursor()));
        }

        long chatId = pagination.getChatId();
        int paginationSize = pagination.getItemIdsSize();

        List<Long> missingUserIds = new ArrayList<>();
        List<Long> missingMemberIds = new ArrayList<>();

        Map<Long, FullUserDTO> userMap = new HashMap<>(paginationSize);
        Map<Long, LightChatMemberDTO> memberMap = new HashMap<>(paginationSize);

        // получаем из кеша User и ChatMember, если есть
        for (long userId : userIds) {
            // User
            Optional<CacheUser> cachedUser = cacheService.getCacheUser(userId);
            if (cachedUser.isPresent()) {
                CacheUser user = cachedUser.get();
                if (user.isDeleted()) continue;

                userMap.put(userId, EntityMapper.toFullDTO(cachedUser.get()));
            } else {
                missingUserIds.add(userId);
            }

            // ChatMember
            Optional<CacheChatMember> cachedMember = cacheService.getChatMember(chatId, userId);
            if (cachedMember.isPresent()) {
                CacheChatMember member = cachedMember.get();
                if (member.isDeleted()) continue;

                memberMap.put(userId, EntityMapper.toLightDTO(member));
            } else {
                missingMemberIds.add(userId);
            }
        }

        // получаем User из бд
        if (!missingUserIds.isEmpty()) {
            List<User> dbUsers = dbService.getActiveUserByIds(missingUserIds);
            for (User user : dbUsers) {
                cacheUser(user); // кешируем
                userMap.put(user.getId(), EntityMapper.toFullDTO(user));
            }
        }

        // получаем ChatMember из бд
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbService.getActiveChatMembersByIds(chatId, missingMemberIds);
            for (ChatMember member : dbMembers) {
                cacheChatMember(member); // кешируем
                memberMap.put(member.getUserId(), EntityMapper.toLightDTO(member));
            }
        }

        // Формируем результат
        Map<Long, FullChatMemberDTO> result = userIds.stream()
                .collect(Collectors.toMap(
                    Function.identity(),
                    userId -> EntityMapper.toFullDTO(userMap.get(userId), memberMap.get(userId)),
                    (a, b) -> b, LinkedHashMap::new
                ));

        return Optional.of(new ChatMembersPageDTO(result, pagination.getNextCursor()));
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
        log.debug("[🏛️] Token {} loaded || getVerificationToken", token);
        optTokenDB.ifPresent(verificationTokenDB -> {
            cacheService.saveVerificationToken(EntityMapper.toCache(verificationTokenDB));
            log.debug("[⚡] Token {} loaded || getVerificationToken", token);
        });
        return optTokenDB.map(EntityMapper::toDTO);
    }
    public int cleanupExpiredTokensFromDB() {
        return dbService.cleanupExpiredVerificationTokens();  // синхронно из бд
    }


    // ========== MESSAGE METHODS ==========


    // Основные методы
    public void saveMessage(LightMessageDTO message) {
        // ищем в кеше контейнер сообщений (если нет, то грузим)
        Optional<CacheChat> cacheChatOpt = getOrLoadCacheChat(message.getChatId());
        if (cacheChatOpt.isEmpty()){
            log.warn("[🏛️] Full chat {} not found || saveMessage", message.getChatId());
            return;
        }

        // асинхронно в бд
        dbService.saveMessageAsync(EntityMapper.toEntity(message));

        // сохраняем в кеш
        cacheChatOpt.get().addNewMessage(EntityMapper.toCache(message));

    } // TODO: SYNC OUTBOX||KAFKA
    public void markMessageAsRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        dbService.markMessageAsRead(chatId, userId, messageId, readAt); // синхронно в бд
        cacheService.updateLastReadByUser(chatId, userId, messageId); // сохраняем в кеш
    }
    public void deleteMessage(long chatId, long messageId) {
        dbService.deleteMessage(messageId); // синхронно в бд
        cacheService.deleteMessage(chatId, messageId); // сохраняем в кеш
    }


    // Вспомогательные методы
    public enum Direction {
        FORWARD,   // после указанного ID (новые сообщения)
        BACKWARD   // до указанного ID (старые сообщения)
    }
    public MessagesPageDTO getChatMessagesPage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        // Загружаем сообщения
        List<LightMessageDTO> messagesWithExtra = getChatMessagesBatch(chatId, userId, cursor, limit + 1, direction); // берем на одну больше (limit + 1)

        boolean hasMore = messagesWithExtra.size() > limit;

        List<LightMessageDTO> messages = messagesWithExtra;
        Long nextCursor = null;

        if (hasMore) {
            if (direction == Direction.FORWARD) {
                // При FORWARD первые limit сообщений - нужные, последнее - для определения nextCursor
                messages = messages.subList(0, limit);
                nextCursor = messages.getLast().getId(); // последнее из нужных

            } else {
                // При BACKWARD последние limit сообщений - нужные, первое - для определения nextCursor
                messages = messages.subList(messages.size() - limit, messages.size());
                nextCursor = messages.getFirst().getId(); // первое из нужных
            }
        }

        Map<Long, LightMessageDTO> messageMap = new LinkedHashMap<>();
        for (LightMessageDTO message : messages) {
            messageMap.put(message.getId(), message);
        }

        return new MessagesPageDTO(messageMap, nextCursor);
    }
    private List<LightMessageDTO> getChatMessagesBatch(long chatId, long userId, Long fromMessageId, int limit, Direction direction) {
        List<LightMessageDTO> result = new ArrayList<>();
        int remainingLimit = limit;
        Long currentFromId = fromMessageId;

        CacheChat cacheChat = getOrLoadCacheChat(chatId).orElse(null);
        if (cacheChat == null) {
            log.warn("[🏛️] Chat {} not found || getChatMessages", chatId);
            return Collections.emptyList();
        }

        // Обновляем статус прочтения пользователя, если его нет в кэше
        if (!cacheChat.hasUserReadStatus(userId)) {
            Optional<Long> userLastReadMessageId = dbService.getUserReadStatusByChatId(chatId, userId);
            cacheChat.updateLastReadByUser(userLastReadMessageId.orElse(-1L), userId);
        }

        // Пытаемся получить сообщения из кэша
        if (cacheChat.hasMessages()) {
            List<CacheMessage> cachedMessages = getMessagesFromCache(
                cacheChat, currentFromId, remainingLimit, direction
            );

            if (!cachedMessages.isEmpty()) {
                result.addAll(cachedMessages.stream().map(m ->
                        EntityMapper.toLightDTO(m, cacheChat.isReadByUser(m.getId(), userId))).toList());

                remainingLimit -= cachedMessages.size();
                if (remainingLimit <= 0) {
                    log.debug("[⚡] Got {} messages from cache for chat {} || getChatMessages", cachedMessages.size(), chatId);
                    return result;
                }

                // Обновляем currentFromId для следующей загрузки
                if (direction == Direction.FORWARD) {
                    // При загрузке вперед (новые сообщения) последнее - самое новое
                    currentFromId = cachedMessages.getLast().getId();
                } else {
                    // При загрузке назад (старые сообщения) последнее - самое старое
                    currentFromId = cachedMessages.getFirst().getId();
                }

                log.debug("[⚡] Got {} messages from cache for chat {}, need {} more || getChatMessages", cachedMessages.size(), chatId, remainingLimit);
            }
        }

        // Загружаем недостающие сообщения из БД
        if (remainingLimit > 0) {
            List<UserMessageDBResult> dbResults = getMessagesFromDB(
                cacheChat, userId, currentFromId, remainingLimit, direction
            );
            log.debug("[🏛️] Loaded {} messages from DB for chat {} || getChatMessages", dbResults.size(), chatId);

            if (!dbResults.isEmpty()) {
                List<LightMessageDTO> newMessages = dbResults.stream().map(EntityMapper::toLightDTO).toList();

                // Добавляем в результат с учетом направления
                if (direction == Direction.BACKWARD) {
                    result.addAll(0, newMessages);
                } else {
                    result.addAll(newMessages);
                }

                // Кешируем загруженные сообщения
                cacheMessages(cacheChat, dbResults);
            }
        }

        return result;
    }
    private List<CacheMessage> getMessagesFromCache(CacheChat cacheChat, Long fromMessageId, int limit, Direction direction) {
        if (fromMessageId == null) {
            return cacheChat.getFirstMessages(limit);
        }

        if (direction == Direction.FORWARD) {
            // Проверяем, загружены ли сообщения после fromMessageId
            if (cacheChat.isLoadedAfter(fromMessageId)) {
                return cacheChat.getMessagesAfter(fromMessageId, limit);
            }
        } else {
            // BACKWARD
            if (cacheChat.isLoadedBefore(fromMessageId)) {
                return cacheChat.getMessagesBefore(fromMessageId, limit);
            }
        }

        return Collections.emptyList();
    }
    private List<UserMessageDBResult> getMessagesFromDB(CacheChat cacheChat, long userId, Long fromMessageId, int limit, Direction direction) {
        long chatId = cacheChat.getId();
        if (fromMessageId == null) {
            return dbService.getChatMessagesFirst(chatId, userId, limit);
        }

        if (direction == Direction.FORWARD) {
            // Загружаем сообщения после fromMessageId
            Long oldestLoadedId = cacheChat.getOldestId();

            // Проверяем, есть ли разрыв между fromMessageId и oldestLoadedId
            if (oldestLoadedId != null && fromMessageId > oldestLoadedId && cacheChat.hasMessages()) {
                // Есть разрыв - используем умную загрузку
                return dbService.getMessagesWithGapCheckAfter(
                    chatId, userId, fromMessageId, oldestLoadedId, limit, 200
                );
            } else {
                return dbService.getChatMessagesAfter(chatId, userId, fromMessageId, limit);
            }
        } else {
            // BACKWARD - загружаем сообщения до fromMessageId
            Long newestLoadedId = cacheChat.getNewestId();

            // Проверяем, есть ли разрыв между fromMessageId и newestLoadedId
            if (newestLoadedId != null && fromMessageId < newestLoadedId && cacheChat.hasMessages()) {
                // Есть разрыв - используем умную загрузку
                return dbService.getMessagesWithGapCheckBefore(
                    chatId, userId, newestLoadedId, fromMessageId, limit, 200
                );
            } else {
                return dbService.getChatMessagesBefore(chatId, userId, fromMessageId, limit);
            }
        }
    }


    // Методы для истории чатов
    public ChatStatsDBResult getChatClearStats(long chatId, long userId) {
        return dbService.getChatMessagesDeletedStats(chatId, userId);
    }


    // ========== CACHE METHODS ==========


    // Основные методы
    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }
    public void printCacheStatus() {
        cacheService.printCacheStats();
    }


    // ========== SUB METHODS ==========


    @Scheduled(initialDelay = 10_000, fixedRate = 86_400_000) // Каждые 24 часа
    public void cleanupExpiredTokens() {
        try {
            int numDeletedTokens = cleanupExpiredTokensFromDB();
            log.info("[🔧] ✅ Expired tokens cleanup completed. Deleted --> {} tokens", numDeletedTokens);
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error during token cleanup: {}", e.getMessage());
        }
    }
}