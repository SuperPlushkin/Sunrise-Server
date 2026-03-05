package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.*;
import com.Sunrise.Entities.Cache.CacheVerificationToken;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Entities.DTO.*;
import com.Sunrise.Entities.Cache.CacheUser;
import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import com.Sunrise.Entities.EntityMapper;

import com.Sunrise.DTO.Paginations.ChatMembersPagination;
import com.Sunrise.DTO.Paginations.UsersPagination;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
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
    public void saveUser(long id, String username, String name, String email, String hashPassword, boolean isEnabled) {
        User user = new User(id, username, name, email, hashPassword, isEnabled);
        CacheUser cacheUser = EntityMapper.toCache(user);

        // сохраняем в кеш
        cacheService.saveUser(cacheUser);

        // инвалидируем кеш поиска
        cacheService.invalidateUsersPagination();

        // асинхронно в бд
        dbService.saveUserAsync(user);
    }
    public void enableUser(Long userId) {
        cacheService.updateUserIsEnabled(userId, true); // сохраняем в кеш

        // инвалидируем кеш поиска
        cacheService.invalidateUsersPagination();

        dbService.enableUserAsync(userId); // асинхронно в бд
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        cacheService.updateUserLastLogin(username, lastLogin); // сохраняем в кеш
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
    }
    public void deleteUser(Long userId) {
        cacheService.deleteUser(userId); // сохраняем в кеш

        // инвалидируем пагинацию удаленного пользователя
        cacheService.invalidateUsersPagination();
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for deleted user {} | deleteUser", userId);

        dbService.deleteUserAsync(userId); // асинхронно в бд
    }
    public void restoreUser(Long userId) {
        cacheService.restoreUser(userId); // сохраняем в кеш

        // инвалидируем пагинацию восстановленного пользователя
        cacheService.invalidateUsersPagination();
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for restored user {}", userId);

        dbService.restoreUserAsync(userId); // асинхронно в бд
    }


    // Вспомогательные методы
    public Optional<FullUserDTO> getUser(Long userId) {
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
                .userIds(pageResult.users().keySet())
                .createdAt(LocalDateTime.now())
                .hasMore(pageResult.hasMore())
                .totalCount(pageResult.totalCount())
                .build()
        );
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
    public void savePersonalChatAndAddPerson(long chatId, long creatorId, long userToAddId) {
        Chat chat = Chat.createPersonalChat(chatId, creatorId);
        ChatMember creator = new ChatMember(chatId, creatorId, true);
        ChatMember member = new ChatMember(chatId, userToAddId, false);

        // сохраняем в кеш
        cacheService.saveNewPersonalChat(
            EntityMapper.toCache(chat),
            EntityMapper.toCache(creator),
            EntityMapper.toCache(member)
        );

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatsPositionChanged(Set.of(creator.getUserId(), member.getUserId()));
        log.debug("[⚡] Invalidating pagination cache for users --> {}, {} | savePersonalChatAndAddPerson", creator.getUserId(), member.getUserId());

        // асинхронно в бд
        dbService.savePersonalChatAsync(chat, userToAddId);
    }
    public void saveGroupChatAndAddPeople(long chatId, String chatName, long creatorId, Set<Long> members) {
        Chat chat = Chat.createGroupChat(chatId, chatName, members.size(), creatorId);

        List<CacheChatMember> cacheChatMembers = members.stream().map(
            memberId -> new CacheChatMember(memberId, chatId, false)
        ).toList();

        // сохраняем в кеш
        cacheService.saveNewGroupChat(
            EntityMapper.toCache(chat),
            cacheChatMembers
        );

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatsPositionChanged(members);
        log.debug("[⚡] Invalidated pagination cache for {} users | saveGroupChatAndAddPeople", members.size());

        // асинхронно в бд
        dbService.saveGroupChatAsync(chat, members.toArray(Long[]::new));
    }
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
        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Chat {} loaded || ensureChatIsValid", chatId);
        dbChat.ifPresent(this::loadDBChatToCache);
        return dbChat.map(Chat::isActive).orElse(false); // восстанавливаем в кеш
    }

    public Optional<ChatDTO> getActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChatCache(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toDTO);

        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Loaded chat {} || getActiveChat", chatId);
        dbChat.ifPresent(this::loadDBChatToCache);
        return dbChat.filter(Chat::isActive).map(EntityMapper::toDTO);
    }
    public Optional<ChatDTO> getPersonalChat(long userId1, long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(EntityMapper::toDTO);

        // грузим из бд
        Optional<Chat> dbChat = dbService.findPersonalChat(userId1, userId2);
        log.debug("[🏛️] Loaded personal chat from users {}, {} || getPersonalChat", userId1, userId2);
        dbChat.ifPresent(this::loadDBChatToCache);
        return dbChat.map(EntityMapper::toDTO);
    }

    public List<ChatDTO> getUserChatsPage(long userId, int offset, int limit) {
        // пробуем кеш
        Optional<CacheService.UserChatsPagination> cached = cacheService.findUserChatsPagination(userId, offset, limit);
        if (cached.isPresent()) {
            CacheService.UserChatsPagination pagination = cached.get();
            List<ChatDTO> chats = getUserChatsBatch(pagination.chatIds());
            log.debug("[⚡] Cache hit for user {} chats page {}/{}", userId, offset, limit);
            return chats;
        }

        log.debug("[🏛️] Loading user {} chats page {}/{} from DB", userId, offset, limit);

        // получаем пагинацию и сохраняем в кеш
        ChatsPageResult pageResult = dbService.getUserChatPage(userId, offset, limit);
        cacheService.saveUserChatsPagination(
                CacheService.UserChatsPagination.builder()
                        .id(randomId())
                        .userId(userId)
                        .offset(offset)
                        .limit(limit)
                        .chatIds(pageResult.getChatIds())
                        .createdAt(LocalDateTime.now())
                        .hasMore(pageResult.getHasMore())
                        .totalCount(pageResult.getTotalCount())
                        .build()
        );

        // загружаем чаты по ID
        return getUserChatsBatch(pageResult.getChatIds());
    }
    private List<ChatDTO> getUserChatsBatch(List<Long> chatIds) {
        if (chatIds.isEmpty())
            return Collections.emptyList();

        Map<Long, ChatDTO> chatMap = new HashMap<>(chatIds.size());
        List<Long> missingIds = new ArrayList<>(chatIds.size() / 2);

        // получаем из кеша Chat, если есть
        for (Long chatId : chatIds) {
            Optional<CacheChat> cachedChat = cacheService.getChatCache(chatId);
            if (cachedChat.isPresent()) {
                CacheChat chat = cachedChat.get();
                chatMap.put(chat.getId(), EntityMapper.toDTO(chat)); // добавляем в массив
            } else {
                missingIds.add(chatId);
            }
        }

        // получаем из бд
        if (!missingIds.isEmpty()) {
            List<Chat> dbChats = dbService.getChatsByIds(missingIds);
            for (Chat chat : dbChats) {
                cacheService.saveExistingChat(EntityMapper.toCache(chat)); // кешируем
                chatMap.put(chat.getId(), EntityMapper.toDTO(chat)); // добавляем в массив
            }
            log.debug("[🏛️] Loaded {} missing chats from DB: {}", missingIds.size(), missingIds);
        }

        // сортируем
        return chatIds.stream().map(chatMap::get).toList();
    }


    public Optional<Boolean> isGroupChat(long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.isActiveGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Chat {} loaded || isGroupChat", chatId);
        dbChat.ifPresent(this::loadDBChatToCache); // восстанавливаем кеш
        return dbChat.map(Chat::isGroup);
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
    private void loadDBChatToCache(Chat chat) {
        cacheService.saveExistingChat(EntityMapper.toCache(chat)); // сохраняем чат в кеш
        log.debug("[⚡] Loaded {} chat {} || loadChatToCache", chat.isGroup() ? "group" : "personal", chat.getId());
    }
    private Optional<Chat> getOrLoadChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChatCache(chatId);
        if (cacheChat.isPresent())
            return cacheChat.map(EntityMapper::toEntity);

        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Loaded chat {} || getChat", chatId);
        dbChat.ifPresent(this::loadDBChatToCache);
        return dbChat;
    }
    private Optional<Chat> getOrLoadActiveChat(long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChatCache(chatId);
        if (cacheChat.isPresent())
            return cacheChat.filter(CacheChat::isActive).map(EntityMapper::toEntity);

        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Loaded chat {} || getActiveDBChat", chatId);
        dbChat.ifPresent(this::loadDBChatToCache);
        return dbChat.filter(Chat::isActive);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveChatMember(long chatId, long userId) {
        // загружаем информацию о чате
        Optional<Chat> chat = getOrLoadChat(chatId);
        if (chat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || saveChatMember", chatId);
            return;
        }

        ChatMember chatMember = new ChatMember(chatId, userId, false);

        // сохраняем в кеш
        cacheService.addNewChatMember(
            EntityMapper.toCache(chat.get()),
            EntityMapper.toCache(chatMember)
        );

        // ИНВАЛИДАЦИЯ
        cacheService.invalidateAfterMembersChanged(chatId, userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | saveChatMember", userId);

        // асинхронно в бд
        dbService.upsertChatMemberAsync(chatMember);
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
    public boolean hasActiveChatMember(Long chatId, Long userId) {
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
    public ChatMembersPageResult getChatMembersPage(Long chatId, int offset, int limit) {
        // пробуем кеш
        Optional<ChatMembersPagination> pagination = cacheService.findChatMembersPagination(chatId, offset, limit);
        if (pagination.isPresent()) {
            Optional<ChatMembersPageResult> members = getChatMembersBatch(pagination.get(), chatId);
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
                    .chatMembersIds(pageResult.chatMembers().keySet())
                    .createdAt(LocalDateTime.now())
                    .hasMore(pageResult.hasMore())
                    .totalCount(pageResult.totalCount())
                    .build()
        );
        log.debug("[🏛️] Loading chat {} members page {}/{} from DB || getChatMembersPage", chatId, offset, limit);

        // загружаем также с бд
        return pageResult;
    }
    private Optional<ChatMembersPageResult> getChatMembersBatch(ChatMembersPagination pagination, Long chatId) {
        if (pagination.isEmptyChatMembersIds())
            return Optional.empty();

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
        for (Long userId : pagination.chatMembersIds()) {
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
                    userId -> EntityMapper.toFullDTO(userMap.get(userId), memberMap.get(userId))
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


    public void saveMessage(Message message) {
        dbService.saveMessageAsync(message);
    }

    public List<MessageDBResult> getChatMessagesUpToDate(Long chatId, Long userId, Integer limit) {
        return dbService.getChatMessagesFirst(chatId, userId, limit);
    }
    public List<MessageDBResult> getChatMessagesBefore(Long chatId, Long userId, Long messageId, Integer limit) {
        return dbService.getChatMessagesBefore(chatId, userId, messageId, limit);
    }
    public List<MessageDBResult> getChatMessagesAfter(Long chatId, Long userId, Long messageId, Integer limit) {
        return dbService.getChatMessagesAfter(chatId, userId, messageId, limit);
    }

    public Integer getVisibleMessagesCount(Long chatId, Long userId) {
        return dbService.getVisibleMessagesCount(chatId, userId);
    }
    public void markMessageAsRead(Long messageId, Long userId) {
        dbService.markMessageAsRead(messageId, userId);
    }


    // Методы для истории чатов
    public int deleteAllChatMessagesForAll(Long chatId, Long userId) {
        return dbService.deleteAllChatMessagesForAll(chatId, userId);
    }
    public int deleteAllChatMessagesForSelf(Long chatId, Long userId) {
        return dbService.deleteAllChatMessagesForSelf(chatId, userId);
    }
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