package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.*;
import com.Sunrise.DTO.Paginations.UserChatsPagination;
import com.Sunrise.Entities.Cache.*;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Entities.DTO.*;
import com.Sunrise.Entities.EntityMapper;

import com.Sunrise.DTO.Paginations.ChatMembersPagination;
import com.Sunrise.DTO.Paginations.UsersPagination;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataAccessService {

    private final CacheService cacheService;
    private final DBService dbService;

    public DataAccessService(CacheService cacheService, DBService dbService) {
        this.cacheService = cacheService;
        this.dbService = dbService;
    }


    // ========== CACHE METHODS ==========

    @PostConstruct
    public void warmUpCache() {
        // подумать чо буду в при старте загружать
    }
    @PreDestroy
    public void onShutdown() {
        // подумать чо буду при завершении делать
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(FullUserDTO user) {
        // сохраняем в кеш
        cacheService.saveUser(EntityMapper.toCache(user));

        // инвалидируем кеш поиска
        cacheService.invalidateUsersPagination();

        // асинхронно в бд
        dbService.saveUserAsync(EntityMapper.toEntity(user));
    }
    public void enableUser(long userId) {
        cacheService.updateUserIsEnabled(userId, true); // сохраняем в кеш

        // инвалидируем кеш поиска
        cacheService.invalidateUsersPagination();

        dbService.enableUserAsync(userId); // асинхронно в бд
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        cacheService.updateUserLastLogin(username, lastLogin); // сохраняем в кеш
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
    }
    public void deleteUser(long userId) {
        cacheService.deleteUser(userId); // сохраняем в кеш

        // инвалидируем пагинацию удаленного пользователя
        cacheService.invalidateUsersPagination();
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for deleted user {} | deleteUser", userId);

        dbService.deleteUserAsync(userId); // асинхронно в бд
    }
    public void restoreUser(long userId) {
        cacheService.restoreUser(userId); // сохраняем в кеш

        // инвалидируем пагинацию восстановленного пользователя
        cacheService.invalidateUsersPagination();
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for restored user {}", userId);

        dbService.restoreUserAsync(userId); // асинхронно в бд
    }


    // Вспомогательные методы
    public Optional<FullUserDTO> getUser(long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getCacheUser(userId);
        if (cached.isPresent())
            return cached.map(EntityMapper::toFullDTO);

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        log.debug("[🏛️] Loaded user {} || getUser", userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || getUser", user.getId());
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
        log.debug("[🏛️] Loaded user with username <<{}>> || getUserByUsername", username);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || getUserByUsername", user.getId());
        });
        return dbUser.map(EntityMapper::toFullDTO);
    }

    public UsersPageResult getUsersPage(String filter, int offset, int limit) {
        // Пробуем найти в кеше
        Optional<UsersPagination> cached = cacheService.findUsersPagination(filter, offset, limit);
        if (cached.isPresent()) {
            UsersPagination pagination = cached.get();
            Optional<UsersPageResult> users = getUsersByPagination(pagination);
            if (users.isPresent()){
                log.debug("[⚡] Cache hit for users page filter='{}' {}/{}", filter, offset, limit);
                return users.get();
            } else {
                cacheService.invalidateUsersPagination();
            }
        }

        // получаем пагинацию и сохраняем в кеш
        UsersPageResult pageResult = dbService.getFullFilteredUsersPage(filter, offset, limit);
        cacheService.saveUsersPagination(
            UsersPagination.builder()
                .id(randomId())
                .filter(filter)
                .offset(offset)
                .limit(limit)
                .userIds(pageResult.getUsersId())
                .createdAt(LocalDateTime.now())
                .hasMore(pageResult.hasMore())
                .totalCount(pageResult.totalCount())
                .build()
        );
        // TODO: МОЖНО ЗАКЕШИРОВАТЬ USERs
        log.debug("[🏛️] Loading users page filter='{}' {}/{} from DB", filter, offset, limit);

        // загружаем пользователей по ID
        return pageResult;
    }
    private Optional<UsersPageResult> getUsersByPagination(UsersPagination pagination) {
        Set<Long> userIds = pagination.userIds();
        List<Long> missingIds = new ArrayList<>(userIds.size() / 2);
        Map<Long, LightUserDTO> userMap = new HashMap<>();

        // получаем из кеша
        for (Long id : userIds) {
            Optional<CacheUser> cachedUser = cacheService.getCacheUser(id);
            if (cachedUser.isPresent()) {
                CacheUser cacheUser = cachedUser.get();
                if (cacheUser.isDeleted())
                    return Optional.empty();

                userMap.put(id, EntityMapper.toLightDTO(cacheUser));
            } else {
                missingIds.add(id);
            }
        }

        // получаем из бд
        if (!missingIds.isEmpty()) {
            List<User> dbUsers = dbService.getUsersByIds(missingIds);
            if (dbUsers.size() != missingIds.size())
                return Optional.empty();

            for (User user : dbUsers) {
                cacheService.saveUser(EntityMapper.toCache(user)); // кешируем
                userMap.put(user.getId(), EntityMapper.toLightDTO(user));
            }
            log.debug("[🏛️] Loaded {} missing users from DB: {} || getUsersByIds", missingIds.size(), missingIds);
        }

        // отдаем
        return Optional.of(new UsersPageResult(userMap, pagination.totalCount(), pagination.hasMore()));
    }

    public boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheService.existsUserByUsername(username))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        log.debug("[🏛️] Loaded user with username <<{}>> || existsUserByUsername", username);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || existsUserByUsername", user.getId());
        });
        return dbUser.isPresent();
    }
    public boolean existsUserByEmail(String email)  {
        // проверяем в кеше
        if (cacheService.existsUserByEmail(email))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByEmail(email);
        log.debug("[🏛️] Loaded user with email <<{}>> || existsUserByEmail", email);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(EntityMapper.toCache(user)); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || existsUserByEmail", user.getId());
        });
        return dbUser.isPresent();
    }


    // ========== LOGIN HISTORY METHODS ==========

    public void saveLoginHistory(LoginHistory loginHistory) {
        dbService.saveLoginHistoryAsync(loginHistory); // асинхронно в бд
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void savePersonalChatAndAddPerson(LightChatDTO chat, LightChatMemberDTO creator, LightChatMemberDTO opponent) {
        // сохраняем в кеш
        cacheService.saveNewPersonalChat(
            EntityMapper.toCache(chat),
            creator.getUserId(),
            opponent.getUserId()
        );

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatsPositionChanged(List.of(creator.getUserId(), opponent.getUserId()));
        log.debug("[⚡] Invalidating pagination cache for users --> {}, {} | savePersonalChatAndAddPerson", creator.getUserId(), opponent.getUserId());

        // асинхронно в бд
        dbService.savePersonalChatAsync(EntityMapper.toEntity(chat), opponent.getChatId());
    } // TODO: ПОДУМАТЬ НАД ФУНКЦИЕЙ СОХРАНЕНИЯ В БД
    public void saveGroupChatAndAddPeople(LightChatDTO chat, List<LightChatMemberDTO> members) {
        // сохраняем в кеш
        CacheChat cacheChat = EntityMapper.toCache(chat);
        cacheService.saveNewGroupChat(cacheChat);
        cacheService.addNewChatMembers(cacheChat, members.stream().map(EntityMapper::toCache).toList());

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatsPositionChanged(members);
        log.debug("[⚡] Invalidated pagination cache for {} users | saveGroupChatAndAddPeople", members.size());

        // асинхронно в бд
        Long[] memberIds = members.stream().map(LightChatMemberDTO::getUserId).toArray(Long[]::new);
        dbService.saveGroupChatAsync(EntityMapper.toEntity(chat), memberIds);
    } // TODO: ПОДУМАТЬ НАД ФУНКЦИЕЙ СОХРАНЕНИЯ В БД
    public void restoreChat(long chatId) {
        // Получаем всех участников чата до восстановления
        List<Long> membersIds = dbService.getChatMemberIds(chatId);

        // сохраняем в кеш
        cacheService.restoreChat(chatId);

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatsPositionChanged(membersIds);
        log.debug("[⚡] Invalidated pagination cache for {} users | restoreChat", membersIds.size());

        // асинхронно в бд
        dbService.restoreChatAsync(chatId);
    }
    public void deleteChat(long chatId) {
        // Получаем всех участников чата до удаления
        List<Long> membersIds = dbService.getChatMemberIds(chatId);

        // сохраняем в кеш
        cacheService.deleteChat(chatId);

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatsPositionChanged(membersIds);
        log.debug("[⚡] Invalidated pagination cache for {} users | deleteChat", membersIds.size());

        // асинхронно в бд
        dbService.deleteChatAsync(chatId);
    }


    // Вспомогательные методы
    public boolean ensureActiveChat(long chatId) {
        // пробуем кеш
        if (cacheService.isActiveChat(chatId))
            return true;

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        log.debug("[🏛️] Full chat {} loaded || ensureChatIsValid", chatId);
        dbFullChat.ifPresent(this::loadFullChatToCache); // восстанавливаем в кеш
        return dbFullChat.map(FullChatResult::getChat).map(Chat::isActive).orElse(false);
    }

    public Optional<LightChatDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getCacheChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toLightDTO);

        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        log.debug("[🏛️] Loaded full chat {} || getActiveChat", chatId);
        dbFullChat.ifPresent(this::loadFullChatToCache);
        return dbFullChat.map(FullChatResult::getChat).filter(Chat::isActive).map(EntityMapper::toLightDTO);
    }
    public Optional<LightChatDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(EntityMapper::toLightDTO);

        // грузим из бд
        Optional<Chat> dbChat = dbService.findPersonalChat(userId1, userId2);
        log.debug("[🏛️] Loaded personal chat from users {}, {} || getPersonalChat", userId1, userId2);
//        dbChat.ifPresent(this::loadDBChatToCache);
        return dbChat.map(EntityMapper::toLightDTO);
    } // TODO: ДОБАВИТЬ В CHAT USER-ID-1 и USER-ID-2

    public UserChatsPageResult getUserChatsPage(long userId, int offset, int limit) {
        // пробуем кеш
        Optional<UserChatsPagination> cached = cacheService.findUserChatsPagination(userId, offset, limit);
        if (cached.isPresent()) {
            Optional<UserChatsPageResult> result = getUserChatsBatch(cached.get());
            if (result.isPresent()) {
                log.debug("[⚡] Cache hit for user {} chats page {}/{}", userId, offset, limit);
                return result.get();
            } else {
                log.debug("[⚡] Cache invalid for user {} chats page {}/{}, loading from DB", userId, offset, limit);
                cacheService.invalidateUserChatsPagination(userId);
            }
        }

        // загружаем с бд и сохраняем пагинацию
        UserChatsPageResult pageResult = dbService.getFullUserChatsPage(userId, offset, limit);
        cacheService.saveUserChatsPagination(
                UserChatsPagination.builder()
                    .id(randomId())
                    .userId(userId)
                    .offset(offset)
                    .limit(limit)
                    .chatIds(pageResult.getChatsId())
                    .createdAt(LocalDateTime.now())
                    .hasMore(pageResult.hasMore())
                    .totalCount(pageResult.totalCount())
                    .build()
        );
        // TODO: МОЖНО ЗАКЕШИРОВАТЬ CHATs, MESSAGEs (CONTAINERs)
        log.debug("[🏛️] Loading user {} chats page {}/{} from DB", userId, offset, limit);
        return pageResult;
    }
    private Optional<UserChatsPageResult> getUserChatsBatch(UserChatsPagination pagination) {
        Set<Long> chatIds = pagination.chatIds();
        if (chatIds.isEmpty()) {
            return Optional.of(new UserChatsPageResult(Collections.emptyMap(), pagination.totalCount(), pagination.hasMore()));
        }

        Set<Long> missingChatIds = new HashSet<>(chatIds.size() / 2);
        Set<Long> missingUserChatIds = new HashSet<>(chatIds.size() / 2);
        Set<Long> missingMessageChatIds = new HashSet<>(chatIds.size() / 2);

        Map<Long, LightChatDTO> chatMap = new HashMap<>(chatIds.size());
        Map<Long, LightUserDTO> userMap = new HashMap<>(chatIds.size());
        Map<Long, LightMessageDTO> messageMap = new HashMap<>(chatIds.size());

        // Пытаемся получить чаты из кэша
        for (Long chatId : chatIds) {
            Optional<CacheChat> cachedChat = cacheService.getCacheChat(chatId);
            if (cachedChat.isPresent()) {
                CacheChat chat = cachedChat.get();
                if (chat.isDeleted()) return Optional.empty();
                if (!chat.isGroup()) missingUserChatIds.add(chatId);
                if (cacheService.getMessagesContainer(chatId).isEmpty()) missingMessageChatIds.add(chatId);

                chatMap.put(chatId, EntityMapper.toLightDTO(chat));
            }
            else {
                missingChatIds.add(chatId);
                missingMessageChatIds.add(chatId);
            }
        }

        // Загружаем недостающие чаты из БД
        if (!missingChatIds.isEmpty()) {
            List<Chat> dbChats = dbService.getChatsByIds(missingChatIds);
            if (dbChats.size() != missingChatIds.size()) return Optional.empty(); // Проверяем, что загрузили все, что ожидали

            for (Chat chat : dbChats) {
                if (chat.isDeleted()) return Optional.empty();
                if (!chat.isGroup()) missingUserChatIds.add(chat.getId());

                cacheService.saveExistingChat(EntityMapper.toCache(chat)); // Кэшируем
                chatMap.put(chat.getId(), EntityMapper.toLightDTO(chat));
            }
        }

        // Загружаем недостающих юзеров из БД для личных чатов
        if (!missingUserChatIds.isEmpty()) {
            Map<Long, User> dbOpponents = dbService.findOpponentsForChats(missingUserChatIds, pagination.userId());
            dbOpponents.forEach((chatId, user) -> {
                cacheService.saveUser(EntityMapper.toCache(user));
                userMap.put(chatId, EntityMapper.toLightDTO(user));
            });

            missingUserChatIds.removeAll(dbOpponents.keySet());
            if (!missingUserChatIds.isEmpty()) {
                log.warn("Opponents not found for chats: {}", missingUserChatIds);
            }
        }

        // Пытаемся получить сообщения из кэша
        for (Long chatId : chatIds) {
            if (missingMessageChatIds.contains(chatId)) continue; // Уже знаем, что нет

            Optional<CacheChatMessagesContainer> container = cacheService.getMessagesContainer(chatId);
            if (container.isPresent()) {
                Optional<CacheMessage> lastMessage = container.get().getFirstMessage();
                if (lastMessage.isPresent()) {
                    messageMap.put(chatId, EntityMapper.toLightDTO(lastMessage.get(), pagination.userId()));
                } else {
                    // Контейнер есть, но пустой (нет сообщений)
                    messageMap.put(chatId, null);
                }
            } else {
                missingMessageChatIds.add(chatId);
            }
        }

        // Загружаем недостающие сообщения из БД
        if (!missingMessageChatIds.isEmpty()) {
            // Загружаем последние сообщения для всех чатов одним запросом
            List<MessageDBResult> dbLastMessages = dbService.getLastMessagesForChats(
                missingMessageChatIds, pagination.userId()
            );

            // Создаем мапу для быстрого доступа
            Map<Long, MessageDBResult> lastMessageMap = dbLastMessages.stream()
                    .collect(Collectors.toMap(MessageDBResult::getChatId, m -> m));

            // Для каждого чата, где нет сообщения, ставим null
            for (Long chatId : missingMessageChatIds) {
                MessageDBResult dbMsg = lastMessageMap.get(chatId);
                if (dbMsg != null) {
                    messageMap.put(chatId, EntityMapper.toLightDTO(dbMsg, pagination.userId(), chatId));
                    cacheLastMessage(chatId, dbMsg);
                } else {
                    messageMap.put(chatId, null);
                    getOrLoadChat(chatId).ifPresent(val ->
                            cacheService.getOrCreateMessagesContainer(EntityMapper.toCache(val), null, 0));
                }
            }
        }

        // сохраняем порядок вставки и собираем в один класс
        Map<Long, FullChatDTO> finalChatMap = chatIds.stream()
                .filter(chatMap::containsKey)
                .collect(Collectors.toMap(
                    chatId -> chatId,
                    chatId -> {
                        LightChatDTO chat = chatMap.get(chatId);
                        LightMessageDTO message = messageMap.get(chatId);
                        if (chat.isGroup()){
                            return EntityMapper.toFullGroupChatDTO(chat, message);
                        } else {
                            LightUserDTO user = userMap.get(chatId);
                            if (user == null) {
                                log.error("User not found for personal chat: {}", chatId);
                                //noinspection DataFlowIssue
                                return null;
                            }
                            return EntityMapper.toFullPersonalChatDTO(chat, user, message);
                        }
                    },
                    (a, b) -> b, LinkedHashMap::new)
                );

        finalChatMap.values().removeIf(Objects::isNull);

        return Optional.of(new UserChatsPageResult(finalChatMap, pagination.totalCount(), pagination.hasMore()));
    }

    public Optional<Boolean> isGroupChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.isActiveGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        log.debug("[🏛️] Full chat {} loaded || isGroupChat", chatId);
        dbFullChat.ifPresent(this::loadFullChatToCache); // восстанавливаем кеш
        return dbFullChat.map(FullChatResult::getChat).map(Chat::isGroup);
    }
    public Optional<Boolean> isActiveAdminInActiveChat(long chatId, long userId) {
        // пробуем кеш
        Optional<Boolean> cached = cacheService.isActiveAdminInActiveChat(chatId, userId);
        if (cached.isPresent())
            return cached;

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbService.getActiveChatMember(chatId, userId);
        log.debug("[🏛️] ChatMember {} in chat {} loaded || isActiveAdminInActiveChat", userId, chatId);
        dbMember.ifPresent(member -> {
            // загружаем информацию о чате
            Optional<Chat> optChat = getOrLoadActiveChat(chatId);
            if (optChat.isEmpty())
                return;

            // сохраняю в кеш
            cacheService.addChatMember(
                EntityMapper.toCache(optChat.get()),
                EntityMapper.toCache(member)
            );
        });
        return dbMember.map(ChatMember::isAdmin);
    }
    public Optional<Long> findAnotherAdmin(long chatId, long excludeUserId) {
        // пробуем кеш
        Optional<Long> cached = cacheService.getAnotherChatAdminId(chatId, excludeUserId);
        if (cached.isPresent())
            return cached;

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbService.findAnotherActiveChatAdmin(chatId, excludeUserId);
        log.debug("[🏛️] ChatMember exclude {} in chat {} loaded || findAnotherAdmin", excludeUserId, chatId);
        dbMember.ifPresent(member -> {
            // загружаем информацию о чате
            Optional<Chat> optChat = getOrLoadActiveChat(chatId);
            if (optChat.isEmpty())
                return;

            // сохраняю в кеш
            cacheService.addChatMember(
                EntityMapper.toCache(optChat.get()),
                EntityMapper.toCache(member)
            );
        });
        return dbMember.map(ChatMember::getUserId);
    }


    // Приватные методы
    private void loadFullChatToCache(FullChatResult fullChat) {
        CacheChat cacheChat = EntityMapper.toCache(fullChat.getChat());
        cacheService.saveExistingChat(cacheChat);
        cacheService.getOrCreateMessagesContainer(cacheChat, EntityMapper.toCache(fullChat.getNewestMessage()), fullChat.getTotalCount());
        log.debug("[⚡] Cached full chat {} with container ({} messages)", cacheChat.getId(), fullChat.getTotalCount());
    }
    private CacheChatMessagesContainer loadFullChatToCacheAndGetContainer(FullChatResult fullChat) {
        CacheChat cacheChat = EntityMapper.toCache(fullChat.getChat());
        cacheService.saveExistingChat(cacheChat);
        var container = cacheService.getOrCreateMessagesContainer(cacheChat, EntityMapper.toCache(fullChat.getNewestMessage()), fullChat.getTotalCount());
        log.debug("[⚡] Cached full chat {} with container ({} messages)", cacheChat.getId(), fullChat.getTotalCount());
        return container;
    }
    private Optional<Chat> getOrLoadChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getCacheChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.map(EntityMapper::toEntity);

        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        log.debug("[🏛️] Loaded full chat {} || getChat", chatId);
        dbFullChat.ifPresent(this::loadFullChatToCache);
        return dbFullChat.map(FullChatResult::getChat);
    }
    private Optional<Chat> getOrLoadActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getCacheChat(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toEntity);

        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        log.debug("[🏛️] Loaded full chat {} || getActiveDBChat", chatId);
        dbFullChat.ifPresent(this::loadFullChatToCache);
        return dbFullChat.map(FullChatResult::getChat).filter(Chat::isActive);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveChatMember(LightChatMemberDTO chatMember) {
        long userId = chatMember.getUserId();
        long chatId = chatMember.getChatId();

        // загружаем информацию о чате
        Optional<Chat> chat = getOrLoadChat(chatId);
        if (chat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || saveChatMember", chatId);
            return;
        }

        // сохраняем в кеш
        cacheService.addNewChatMember(EntityMapper.toCache(chat.get()), EntityMapper.toCache(chatMember));

        // ИНВАЛИДАЦИЯ
        cacheService.invalidateAfterMembersChanged(chatId, userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | saveChatMember", userId);

        // асинхронно в бд
        dbService.upsertChatMemberAsync(EntityMapper.toEntity(chatMember));
    }
    public void updateChatCreator(long chatId, long newCreatorId) {
        cacheService.updateChatCreator(chatId, newCreatorId); // сохраняем в кеш
        dbService.updateChatCreatorAsync(chatId, newCreatorId); // асинхронно в бд
    }
    public void updateAdminRights(long chatId, long userId, boolean isAdmin) {
        cacheService.saveOrUpdateAdminRights(chatId, userId, isAdmin); // обновляем кэш
        dbService.updateUserAdminRightsAsync(chatId, userId, isAdmin); // асинхронно в бд
    }
    public void removeUserFromChat(long chatId, long userId) {
        cacheService.removeChatMember(userId, chatId); // сохраняем в кеш

        // ИНВАЛИДАЦИЯ
        cacheService.invalidateAfterMembersChanged(chatId, userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | removeUserFromChat", userId);

        dbService.removeUserFromChatAsync(userId, chatId); // асинхронно в бд
    }


    // Вспомогательные методы
    public boolean hasActiveChatMember(long chatId, long userId) {
        // проверка по кешу пользователя
        Optional<Boolean> userChatCheck = cacheService.getCacheUser(userId).map(user -> user.hasChat(chatId));
        if (userChatCheck.isPresent() && userChatCheck.get().equals(true))
            return true;

        // проверка через контейнер участников
        Optional<Boolean> hasActiveChatMember = cacheService.hasActiveChatMember(chatId, userId);
        if (hasActiveChatMember.isPresent())
            return hasActiveChatMember.get();

        // загружаем информацию о чате
        Optional<Chat> chat = getOrLoadChat(chatId);
        if (chat.isEmpty())
            return false;

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbService.getChatMember(chatId, userId);
        if (dbMember.isEmpty())
            return false;

        // кешируем
        CacheChatMember member = EntityMapper.toCache(dbMember.get());
        cacheService.addChatMember(EntityMapper.toCache(chat.get()), member);
        return member.isActive();
    }
    public ChatMembersPageResult getChatMembersPage(long chatId, int offset, int limit) {
        // пробуем кеш
        Optional<ChatMembersPagination> pagination = cacheService.findChatMembersPagination(chatId, offset, limit);
        if (pagination.isPresent()) {
            Optional<ChatMembersPageResult> members = getChatMembersBatch(pagination.get());
            if (members.isPresent()){
                log.debug("[⚡] Cache hit for chat {} members page {}/{}", chatId, offset, limit);
                return members.get();
            } else {
                cacheService.invalidateChatMembersPagination(chatId);
            }
        }

        // загружаем с бд и сохраняем пагинацию
        ChatMembersPageResult pageResult = dbService.getFullChatMembersPage(chatId, offset, limit);
        cacheService.saveChatMembersPagination(
            ChatMembersPagination.builder()
                    .id(randomId())
                    .chatId(chatId)
                    .offset(offset)
                    .limit(limit)
                    .chatMembersIds(pageResult.getMembersId())
                    .createdAt(LocalDateTime.now())
                    .hasMore(pageResult.hasMore())
                    .totalCount(pageResult.totalCount())
                    .build()
        );
        // TODO: МОЖНО ЗАКЕШИРОВАТЬ CHAT-MEMBERS
        log.debug("[🏛️] Loading chat {} members page {}/{} from DB || getChatMembersPage", chatId, offset, limit);
        return pageResult;
    }
    private Optional<ChatMembersPageResult> getChatMembersBatch(ChatMembersPagination pagination) {
        if (pagination.isEmptyChatMembersIds())
            return Optional.empty();

        long chatId = pagination.chatId();

        Chat dbChat = getOrLoadChat(chatId).orElse(null);
        if (dbChat == null)
            return Optional.empty();

        CacheChat chat = EntityMapper.toCache(dbChat);

        int paginationSize = pagination.getSizeChatMembersIds();
        Set<Long> userIds = pagination.chatMembersIds();

        List<Long> missingUserIds = new ArrayList<>();
        List<Long> missingMemberIds = new ArrayList<>();

        Map<Long, FullUserDTO> userMap = new HashMap<>(paginationSize);
        Map<Long, LightChatMemberDTO> memberMap = new HashMap<>(paginationSize);

        // получаем из кеша User и ChatMember, если есть
        for (long userId : pagination.chatMembersIds()) {
            // User
            Optional<CacheUser> cachedUser = cacheService.getCacheUser(userId);
            if (cachedUser.isPresent()) {
                userMap.put(userId, EntityMapper.toFullDTO(cachedUser.get()));
            } else {
                missingUserIds.add(userId);
            }

            // ChatMember
            Optional<CacheChatMember> cachedMember = cacheService.getChatMember(chatId, userId);
            if (cachedMember.isPresent()) {
                CacheChatMember member = cachedMember.get();
                if (member.isDeleted())
                    return Optional.empty();
                memberMap.put(userId, EntityMapper.toLightDTO(member));
            } else {
                missingMemberIds.add(userId);
            }
        }

        // получаем User из бд
        if (!missingUserIds.isEmpty()) {
            List<User> dbUsers = dbService.getUsersByIds(missingUserIds);
            for (User user : dbUsers) {
                cacheService.saveUser(EntityMapper.toCache(user)); // Кешируем
                userMap.put(user.getId(), EntityMapper.toFullDTO(user));
            }
        }

        // получаем ChatMember из бд
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbService.getActiveChatMembersByIds(chatId, missingMemberIds);
            if (dbMembers.size() != paginationSize)
                return Optional.empty();

            for (ChatMember member : dbMembers) {
                cacheService.addChatMember(chat, EntityMapper.toCache(member)); // Кешируем
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


        return Optional.of(new ChatMembersPageResult(result, pagination.totalCount(), pagination.hasMore()));
    }



    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(VerificationTokenDTO verificationTokenDTO) {
        cacheService.saveVerificationToken(EntityMapper.toCache(verificationTokenDTO)); // сохраняем в кеш
        dbService.saveVerificationTokenAsync(EntityMapper.toEntity(verificationTokenDTO)); // асинхронно в бд
    }
    public void deleteVerificationToken(String token) {
        cacheService.deleteVerificationToken(token); // сохраняем в кеш
        dbService.deleteVerificationTokenAsync(token); // асинхронно в бд
    }


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



    // ========== MESSAGE METHODS ========== TODO: ВСЕ ЕЩЕ КОЛХОЗ, ТОЛЬКО БД РАБОТАЕТ


    public void saveMessage(FullMessageDTO message) {
        // загружаем информацию о чате
        Optional<Chat> chat = getOrLoadChat(message.getChatId());
        if (chat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || saveMessage", message.getChatId());
            return;
        }

        // сохраняем в кеш
        cacheService.addNewMessageToContainer(EntityMapper.toCache(chat.get()), EntityMapper.toCache(message));

        // асинхронно в бд
        dbService.saveMessageAsync(EntityMapper.toEntity(message));
    }

    public List<LightMessageDTO> getChatMessagesUpToDate(long chatId, long userId, int limit) {
        return dbService.getChatMessagesFirst(chatId, userId, limit);
    }
    public List<LightMessageDTO> getChatMessagesBefore(long chatId, long userId, long messageId, int limit) {
        return dbService.getChatMessagesBefore(chatId, userId, messageId, limit);
    }
    public List<LightMessageDTO> getChatMessagesAfter(long chatId, long userId, long messageId, int limit) {
        List<LightMessageDTO> result = new ArrayList<>();
        int remainingLimit = limit;
        long currentAfterId = messageId;

        CacheChatMessagesContainer container = getOrCreateMessagesContainer(chatId);

        // Пытаемся получить из кеша
        if (container.hasMessages()) {
            // Если кеш полностью загружен - просто берем из него
            if (container.isFullyLoaded()) {
                List<CacheMessage> cachedMessages = container.getMessagesAfter(messageId, limit);
                log.debug("[⚡] Fully loaded cache hit for chat {}", chatId);
                return cachedMessages.stream().map(m -> EntityMapper.toLightDTO(m, userId)).toList();
            }

            // Пытаемся получить из кеша то, что уже загружено
            if (container.isLoadedAfter(currentAfterId)) {
                List<CacheMessage> cachedMessages = container.getMessagesAfter(currentAfterId, remainingLimit);
                if (!cachedMessages.isEmpty()) {
                    result.addAll(cachedMessages.stream().map(m -> EntityMapper.toLightDTO(m, userId)).toList());

                    remainingLimit -= cachedMessages.size();
                    currentAfterId = cachedMessages.getLast().getId();

                    log.debug("[⚡] Got {} messages from cache for chat {}", cachedMessages.size(), chatId);

                    if (remainingLimit <= 0) return result;
                }
            }
        }

        // Если нужно еще сообщений - идем в БД
        if (remainingLimit > 0) {
            List<MessageDBResult> dbResults = getChatMessagesFromDBAfter(
                chatId, userId, currentAfterId, remainingLimit, container
            );

            if (!dbResults.isEmpty()) {
                // Добавляем в результат
                for (MessageDBResult row : dbResults) {
                    result.add(EntityMapper.toLightDTO(row, userId, chatId));
                }

                // Кешируем
                cacheMessages(container, dbResults);
                log.debug("[🏛️] Loaded and cached {} messages for chat {}", dbResults.size(), chatId);
            }
        }

        return result;
    }
    private List<MessageDBResult> getChatMessagesFromDBAfter(long chatId, long userId, long afterId, int limit, CacheChatMessagesContainer container) {
        long oldestLoadedId = container.getOldestId() != null ? container.getOldestId() : -1;
        if (afterId > oldestLoadedId && oldestLoadedId != -1) {
            // Есть разрыв - загружаем с проверкой
            return dbService.getMessagesWithGapCheckAfter(
                    chatId, userId, afterId, oldestLoadedId, limit, 200
            );
        } else {
            // Обычная загрузка
            return dbService.getChatMessagesAfter(chatId, userId, afterId, limit);
        }
    }

    private CacheChatMessagesContainer getOrCreateMessagesContainer(long chatId) {
        Optional<CacheChatMessagesContainer> containerOpt = cacheService.getMessagesContainer(chatId);
        if (containerOpt.isPresent())
            return containerOpt.get();

        // Контейнера нет - создаем через загрузку полного чата
        Optional<FullChatResult> dbFullChat = dbService.getFullChat(chatId);
        if (dbFullChat.isEmpty())
            throw new RuntimeException("no chat in db");

        log.debug("[🏛️] Full chat {} loaded, creating container", chatId);
        return loadFullChatToCacheAndGetContainer(dbFullChat.get());
    }
    private void cacheMessages(CacheChatMessagesContainer container, List<MessageDBResult> dbResults) {
        if (dbResults.isEmpty()) return;

        // кешируем
        List<CacheMessage> cacheMessages = new ArrayList<>();
        for (MessageDBResult row : dbResults) {
            cacheMessages.add(EntityMapper.toCache(row, container.getChatId()));
        }
        container.addMessages(cacheMessages);
    }


    public void markMessageAsRead(Long chatId, Long messageId, Long userId) {
        cacheService.markMessageAsRead(chatId, messageId, userId);
        dbService.markMessageAsRead(messageId, userId);
    }
    public int getVisibleMessagesCount(Long chatId, Long userId) {
        return dbService.getVisibleMessagesCount(chatId, userId);
    }


    // Методы для истории чатов
    public ChatStatsDBResult getChatClearStats(Long chatId, Long userId) {
        return dbService.getChatMessagesDeletedStats(chatId, userId);
    }


    // ========== CACHE METHODS ==========


    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }
    public void printCacheStatus() {
        cacheService.printCacheStats();
    }


    // ========== SUB METHODS ==========


    public static long randomId() {
        return Math.abs(new SecureRandom().nextLong());
    }
    public static String generate64CharString() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[48]; // 48 bytes = 64 base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

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