package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.DTO.DBResults.ChatsPageResult;
import com.Sunrise.DTO.DBResults.MessageDBResult;
import com.Sunrise.DTO.DBResults.UsersPageResult;
import com.Sunrise.DTO.Responses.ChatDTO;
import com.Sunrise.DTO.Responses.ChatMemberDTO;
import com.Sunrise.Entities.Cache.CacheUser;
import com.Sunrise.Entities.Cache.ChatMembersContainer;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
@Service
public class DataAccessService {

    private static final Logger log = LoggerFactory.getLogger(DataAccessService.class);
    private final CacheService cacheService;
    private final DBService dbService;

    public DataAccessService(CacheService cacheService, DBService dbService) {
        this.cacheService = cacheService;
        this.dbService = dbService;
    }


    // ========== CACHE METHODS ==========

    public void printCacheStats() {
        CacheService.CacheStats stats = cacheService.getCacheStatus();
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


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(User user) {
        cacheService.saveUser(user); // сохраняем в кеш

        cacheService.invalidateUsersPagination(); // Инвалидируем кеш поиска

        dbService.saveUserAsync(user); // асинхронно в бд
    }
    public void enableUser(Long userId) {
        cacheService.updateUserIsEnabled(userId, true); // сохраняем в кеш

        cacheService.invalidateUsersPagination(); // Инвалидируем кеш поиска

        dbService.enableUserAsync(userId); // асинхронно в бд
    }
    public void deleteUser(Long userId) {
        cacheService.deleteUser(userId); // сохраняем в кеш

        // Инвалидируем пагинацию удаленного пользователя
        cacheService.invalidateUsersPagination();
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for deleted user {} | deleteUser", userId);

        dbService.deleteUserAsync(userId); // асинхронно в бд
    }
    public void restoreUser(Long userId) {
        cacheService.restoreUser(userId); // сохраняем в кеш

        // Инвалидируем пагинацию восстановленного пользователя
        cacheService.invalidateUsersPagination();
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for restored user {}", userId);

        dbService.restoreUserAsync(userId); // асинхронно в бд
    }

    // UPDATE методы
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        cacheService.updateUserLastLogin(username, lastLogin); // сохраняем в кеш
        dbService.updateLastLoginAsync(username, lastLogin); // асинхронно в бд
    }

    // GET методы
    public Optional<User> getUser(Long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getCacheUser(userId);
        if (cached.isPresent())
            return cached.map(User::new);

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        log.debug("[🏛️] Loaded user {} || getUser", userId);
        dbUser.ifPresent(user -> {
            loadFullUserToCache(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || getUser", user.getId());
        });
        return dbUser;
    }
    public Optional<User> getUserByUsername(String username) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUserByUsername(username);
        if (cached.isPresent())
            return cached.map(User::new);

        //грузим из бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        log.debug("[🏛️] Loaded user with username <<{}>> || getUserByUsername", username);
        dbUser.ifPresent(user -> {
            loadFullUserToCache(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || getUserByUsername", user.getId());
        });
        return dbUser;
    }
    public Optional<User> getUserByEmail(String email) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getUserByEmail(email);
        if (cached.isPresent())
            return cached.map(User::new);

        // грузим из бд
        Optional<User> dbUser = dbService.getUserByEmail(email);
        log.debug("[🏛️] Loaded user with email <<{}>> || getUserByEmail", email);
        dbUser.ifPresent(user -> {
            loadFullUserToCache(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || getUserByEmail", user.getId());
        });
        return dbUser;
    }
    private List<User> getUsersByIds(List<Long> userIds) {
        if (userIds.isEmpty())
            return Collections.emptyList();

        List<User> result = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        // Сначала собираем из кеша
        for (Long id : userIds) {
            Optional<CacheUser> cachedUser = cacheService.getCacheUser(id);
            if (cachedUser.isPresent()) {
                result.add(new User(cachedUser.get()));
            } else {
                missingIds.add(id);
            }
        }

        // Загружаем недостающие одним запросом
        if (!missingIds.isEmpty()) {
            List<User> dbUsers = dbService.getUsersByIds(missingIds);
            for (User user : dbUsers) {
                cacheService.saveUser(user); // Кешируем
                result.add(user);
            }
            log.debug("[🏛️] Loaded {} missing users from DB: {}", missingIds.size(), missingIds);
        }

        // Восстанавливаем порядок как в userIds
        Map<Long, User> userMap = result.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .toList();
    }
    public Optional<List<User>> getFilteredUsersPage(String filter, int offset, int limit) {
        // Пробуем найти в кеше
        Optional<CacheService.UsersPagination> cached = cacheService.findUsersPagination(filter, offset, limit);

        if (cached.isPresent()) {
            CacheService.UsersPagination pagination = cached.get();
            List<User> users = getUsersByIds(pagination.getUserIds());
            log.debug("[⚡] Cache hit for users page filter='{}' {}/{}", filter, offset, limit);
            return Optional.of(users);
        }

        log.debug("[🏛️] Loading users page filter='{}' {}/{} from DB", filter, offset, limit);

        // ОДИН ЗАПРОС с оконной функцией
        UsersPageResult pageResult = dbService.getFilteredUsersPage(filter, offset, limit);

        // Сохраняем в кеш (только ID!)
        cacheService.saveUsersPagination(
                CacheService.UsersPagination.builder()
                        .id(randomId())
                        .filter(filter)
                        .offset(offset)
                        .limit(limit)
                        .userIds(pageResult.userIds())
                        .createdAt(LocalDateTime.now())
                        .hasMore(pageResult.hasMore())
                        .totalCount(pageResult.totalCount())
                        .build()
        );

        // Загружаем пользователей по ID
        List<User> users = getUsersByIds(pageResult.userIds());
        return Optional.of(users);
    }
    public boolean existsUser(Long userId) {
        // проверяем в кеше
        if (cacheService.existsUser(userId))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUser(userId);
        log.debug("[🏛️] Loaded user {} || existsUserById", userId);
        dbUser.ifPresent(user -> {
            loadFullUserToCache(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || existsUserById", user.getId());
        });
        return dbUser.isPresent();
    }
    public Boolean existsUserByUsername(String username) {
        // проверяем в кеше
        if (cacheService.existsUserByUsername(username))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByUsername(username);
        log.debug("[🏛️] Loaded user with username <<{}>> || existsUserByUsername", username);
        dbUser.ifPresent(user -> {
            loadFullUserToCache(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || existsUserByUsername", user.getId());
        });
        return dbUser.isPresent();
    }
    public Boolean existsUserByEmail(String email)  {
        // проверяем в кеше
        Boolean existsInCache = cacheService.existsUserByEmail(email);
        if (existsInCache)
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUserByEmail(email);
        log.debug("[🏛️] Loaded user with email <<{}>> || existsUserByEmail", email);
        dbUser.ifPresent(user -> {
            loadFullUserToCache(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || existsUserByEmail", user.getId());
        });
        return dbUser.isPresent();
    }

    // Методы для кеша
    private void loadFullUserToCache(User user){
        cacheService.saveUser(user); // сохраняем в кеш
        List<Long> dbChatIds = dbService.getUserChatIds(user.getId()); // получаем id чатов
        log.debug("[🏛️] Loaded {} chatsIds for user {} || loadUserToCache", dbChatIds.size(), user.getId());
        cacheService.updateUserChatsIds(user.getId(), new HashSet<>(dbChatIds)); // затем сохраняем его чаты (только их id)
        log.debug("[⚡] Loaded {} chatsIds for user {} || loadUserToCache", dbChatIds.size(), user.getId());
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    public void saveLoginHistory(LoginHistory loginHistory) {
        dbService.saveLoginHistoryAsync(loginHistory); // асинхронно в бд
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void savePersonalChatAndAddPerson(Chat chat, ChatMember creator, ChatMember member) {
        cacheService.saveNewPersonalChat(chat, creator, member); // сохраняем в кеш

        // Инвалидируем пагинацию для всех участников
        cacheService.invalidateUserChatsPagination(creator.getUserId());
        cacheService.invalidateUserChatsPagination(member.getUserId());
        log.debug("[⚡] Invalidating pagination cache for users --> {}, {}", creator.getUserId(), member.getUserId());

        // асинхронно в бд
        dbService.saveChatAsync(chat);
        dbService.upsertChatMemberAsync(creator);
        dbService.upsertChatMemberAsync(member);
    }
    public void saveGroupChatAndAddPeople(Chat chat, List<ChatMember> members) {
        cacheService.saveNewGroupChat(chat, members); // сохраняем в кеш

        // Инвалидируем пагинацию для всех участников
        members.stream()
                .map(ChatMember::getUserId)
                .forEach(userId -> {
                    log.debug("[⚡] Invalidating pagination cache for user {}", userId);
                    cacheService.invalidateUserChatsPagination(userId);
                });

        // асинхронно в бд
        dbService.saveChatAsync(chat);
        members.forEach(dbService::upsertChatMemberAsync);
    }
    public void restoreChat(Long chatId) {
        // Получаем всех участников чата до восстановления
        List<Long> memberIds = dbService.getChatMemberIds(chatId);

        cacheService.restoreChat(chatId); // сохраняем в кеш

        // Инвалидируем пагинацию для всех участников
        memberIds.forEach(userId -> {
            cacheService.invalidateUserChatsPagination(userId);
            log.debug("[⚡] Invalidated pagination cache for user {}", userId);
        });

        dbService.restoreChatAsync(chatId); // асинхронно в бд
    }
    public void deleteChat(Long chatId) {
        // Получаем всех участников чата до удаления
        List<Long> memberIds = dbService.getChatMemberIds(chatId);

        cacheService.deleteChat(chatId); // сохраняем в кеш

        // Инвалидируем пагинацию для всех участников
        memberIds.forEach(userId -> {
            cacheService.invalidateUserChatsPagination(userId);
            log.debug("[⚡] Invalidated pagination cache for user {}", userId);
        });

        dbService.deleteChatAsync(chatId); // асинхронно в бд
    }


    // Вспомогательные методы
    public boolean ensureChatIsValid(Long chatId) {
        // пробуем кеш
        if (cacheService.existsAndNotDeletedChat(chatId))
            return true;

        // грузим из бд
        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Chat {} loaded || ensureChatIsValid", chatId);
        return dbChat.map(chat -> {
            return !loadChatToCache(chat).getIsDeleted(); // восстанавливаем в кеш
        }).orElse(false);
    }


    public Optional<Chat> getChat(Long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChatCache(chatId);
        if (cacheChat.isPresent())
            return cacheChat.map(Chat::new);

        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Loaded chat {} || getChat", chatId);
        dbChat.ifPresent(this::loadChatToCache);
        return dbChat;
    }
    public Optional<Chat> getPersonalChat(Long userId1, Long userId2) {
        // пробуем кеш
        Optional<CacheChat> cached = cacheService.getPersonalChat(userId1, userId2);
        if (cached.isPresent())
            return cached.map(Chat::new);

        // грузим из бд
        Optional<Chat> dbChat = dbService.findPersonalChat(userId1, userId2);
        log.debug("[🏛️] Loaded personal chat from users {}, {} || getPersonalChat", userId1, userId2);
        dbChat.ifPresent(this::loadChatToCache);
        return dbChat;
    }
    private Optional<CacheChat> getCacheChat(Long chatId) {
        Optional<CacheChat> cacheChat = cacheService.getChatCache(chatId);
        if (cacheChat.isPresent())
            return cacheChat;

        Optional<Chat> dbChat = dbService.getChat(chatId);
        if (dbChat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || reloadChatCache", chatId);
            return Optional.empty();
        }

        Chat chat = dbChat.get();
        log.debug("[🏛️] Loaded {} chat {} || reloadChatCache", chat.getIsGroup() ? "group" : "personal", chat.getId());
        return Optional.of(loadChatToCache(chat));
    }
    private List<Chat> getChatsByIds(List<Long> chatIds) {
        if (chatIds.isEmpty())
            return Collections.emptyList();

        List<Chat> result = new ArrayList<>();
        List<Long> missingIds = new ArrayList<>();

        // Сначала собираем из кеша
        for (Long id : chatIds) {
            Optional<CacheChat> cachedChat = cacheService.getChatCache(id);
            if (cachedChat.isPresent()) {
                result.add(new Chat(cachedChat.get()));
            } else {
                missingIds.add(id);
            }
        }

        // Загружаем недостающие одним запросом
        if (!missingIds.isEmpty()) {
            List<Chat> dbChats = dbService.getChatsByIds(missingIds);
            for (Chat chat : dbChats) {
                cacheService.saveExistingChat(chat);
                result.add(chat);
            }

            log.debug("[🏛️] Loaded {} missing chats from DB: {}", missingIds.size(), missingIds);
        }

        // Восстанавливаем порядок как в chatIds
        Map<Long, Chat> chatMap = result.stream().collect(Collectors.toMap(Chat::getId, Function.identity()));
        return chatIds.stream()
                .map(chatMap::get)
                .filter(Objects::nonNull)
                .toList();
    }

    public Optional<List<ChatDTO>> getUserChats(Long userId) {
        // проверяем что пользователь существует
        if (!existsUser(userId))
            return Optional.empty();

        // есть ВСЕ chatIds в кеше, подгружаем НЕКОТОРЫЕ чаты, если их нет
        List<ChatDTO> result = new ArrayList<>();
        Optional<Set<Long>> cachedChatIds = cacheService.getUserChatsIds(userId);
        if (cachedChatIds.isPresent()) {
            // ищем чаты, которые надо подгрузить с бд
            List<Long> missingChatIds = new ArrayList<>();
            for (Long chatId : cachedChatIds.get()) {
                Optional<CacheChat> cachedChat = cacheService.getChatCache(chatId);
                if (cachedChat.isPresent()) {
                    result.add(new ChatDTO(cachedChat.get()));
                } else {
                    missingChatIds.add(chatId);
                }
            }

            // Загружаем недостающие чаты из БД
            if (!missingChatIds.isEmpty()) {
                List<Chat> dbChats = dbService.getChatsByIds(missingChatIds);
                log.debug("[🏛️] Loaded {} missing chat(s) with members for user {} || getUserChats", missingChatIds.size(), userId);
                dbChats.forEach(chat -> {
                    loadChatToCache(chat);
                    result.add(new ChatDTO(chat));
                });
            }

            return Optional.of(result);
        }

        // НЕТ chatIds в кеше, подгружаем ВСЕ чаты из бд
        List<Chat> userChats = dbService.getUserChats(userId);
        if (!userChats.isEmpty()) {
            log.debug("[🏛️] Loaded {} missing chat(s) with members for user {} || getUserChats", userChats.size(), userId);
            userChats.forEach(chat -> {
                loadChatToCache(chat);
                result.add(new ChatDTO(chat));
            });
        }

        return Optional.of(result);
    }
    public Optional<List<ChatDTO>> getUserChatsPage(Long userId, int offset, int limit) {
        // Пробуем найти в кеше
        Optional<CacheService.UserChatsPagination> cached = cacheService.findUserChatsPagination(userId, offset, limit);

        if (cached.isPresent()) {
            CacheService.UserChatsPagination pagination = cached.get();
            List<Chat> chats = getChatsByIds(pagination.getChatIds());
            log.debug("[⚡] Cache hit for user {} chats page {}/{}", userId, offset, limit);
            return Optional.of(chats.stream().map(ChatDTO::new).toList());
        }

        // Проверяем существование пользователя
        if (!existsUser(userId))
            return Optional.empty();

        log.debug("[🏛️] Loading user {} chats page {}/{} from DB", userId, offset, limit);

        // ОДИН ЗАПРОС с оконной функцией
        ChatsPageResult pageResult = dbService.getUserChatsPage(userId, offset, limit);

        // Сохраняем в кеш пагинации (только ID!)
        cacheService.saveUserChatsPagination(
                CacheService.UserChatsPagination.builder()
                        .id(randomId())
                        .userId(userId)
                        .offset(offset)
                        .limit(limit)
                        .chatIds(pageResult.chatIds())
                        .createdAt(LocalDateTime.now())
                        .hasMore(pageResult.hasMore())
                        .totalCount(pageResult.totalCount())
                        .build()
        );

        // Загружаем чаты по ID
        List<Chat> chats = getChatsByIds(pageResult.chatIds());
        return Optional.of(chats.stream().map(ChatDTO::new).toList());
    }

    public Optional<Boolean> isGroupChat(Long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.getIsGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Chat {} loaded || isGroupChat", chatId);
        return dbChat.map(chat -> loadChatToCache(chat).getIsGroup()); // восстанавливаем кеш
    }
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        // пробуем кеш
        Optional<Boolean> cached = cacheService.isChatAdmin(chatId, userId);
        if (cached.isPresent())
            return cached;

        // загружаем информацию о чате
        Optional<Chat> optChat = getChat(chatId);
        if (optChat.isEmpty())
            return Optional.empty();

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbService.getChatMember(chatId, userId);
        return dbMember.map(member -> {
            cacheService.addChatMember(optChat.get(), member);
            return member.getIsAdmin();
        });
    }
    public Optional<Long> findAnotherAdmin(Long chatId, Long excludeUserId) {
        // пробуем кеш
        Optional<Long> cached = cacheService.getAnotherChatAdminId(chatId, excludeUserId);
        if (cached.isPresent())
            return cached;

        // загружаем информацию о чате
        Optional<Chat> optChat = getChat(chatId);
        if (optChat.isEmpty())
            return Optional.empty();

        // надо найти пользователя, добавить в кеш и отдать
        Optional<ChatMember> dbMember = dbService.getAnotherChatAdmin(chatId, excludeUserId);
        return dbMember.map(member -> {
            cacheService.addChatMember(optChat.get(), member);
            return member.getUserId();
        });
    }


    // Методы для истории чатов
    public Integer clearChatHistoryForAll(Long chatId, Long userId) {
        return dbService.clearChatHistoryForAll(chatId, userId);
    }
    public Integer clearChatHistoryForSelf(Long chatId, Long userId) {
        return dbService.clearChatHistoryForSelf(chatId, userId);
    }
    public ChatStatsDBResult getChatClearStats(Long chatId, Long userId) {
        return dbService.getChatClearStats(chatId, userId);
    }


    // Методы для кеша
    private Optional<CacheChat> loadChatToCacheById(Long chatId) {
        Optional<Chat> dbChat = dbService.getChat(chatId);
        if (dbChat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || loadChatToCacheById", chatId);
            return Optional.empty();
        }

        Chat chat = dbChat.get();
        log.debug("[🏛️] Loaded {} chat {} || loadChatToCacheById", chat.getIsGroup() ? "group" : "personal", chat.getId());
        return Optional.of(loadChatToCache(chat));
    }
    private CacheChat loadChatToCache(Chat chat) {
        var cacheChat = cacheService.saveExistingChat(chat); // сохраняем чат в кеш
        log.debug("[⚡] Loaded {} chat {} || loadChatToCache", cacheChat.getIsGroup() ? "group" : "personal", cacheChat.getId());
        return cacheChat;
    }



    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveChatMember(ChatMember chatMember) {
        // загружаем информацию о чате
        Optional<Chat> chat = getChat(chatMember.getChatId());
        if (chat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || saveChatMember", chatMember.getChatId());
            return;
        }

        cacheService.addNewChatMember(chat.get(), chatMember); // сохраняем в кеш

        // Инвалидируем пагинацию нового участника
        cacheService.invalidateUserChatsPagination(chatMember.getUserId());
        log.debug("[⚡] Invalidated pagination cache for user {} | saveChatMember", chatMember.getUserId());

        dbService.upsertChatMemberAsync(chatMember); // асинхронно в бд
    }
    public void updateChatCreator(Long chatId, Long newCreatorId) {
        cacheService.updateChatCreator(chatId, newCreatorId); // сохраняем в кеш
        dbService.updateChatCreatorAsync(chatId, newCreatorId); // асинхронно в бд
    }
    public void updateAdminRights(Long chatId, Long userId, Boolean isAdmin) {
        cacheService.saveAdminRights(chatId, userId, isAdmin); // обновляем кэш
        dbService.updateAdminRightsAsync(chatId, userId, isAdmin); // асинхронно в бд
    }
    public void removeUserFromChat(Long userId, Long chatId) {
        cacheService.removeChatMember(userId, chatId); // сохраняем в кеш

        // Инвалидируем пагинацию удаленного пользователя
        cacheService.invalidateUserChatsPagination(userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | removeUserFromChat", userId);

        dbService.removeUserFromChatAsync(userId, chatId); // асинхронно в бд
    }


    // Вспомогательные методы
    public Optional<List<ChatMemberDTO>> getChatMembers(Long chatId) {
        // пробуем кеш
        Optional<List<CacheChatMember>> cached = cacheService.getChatMembers(chatId);
        if (cached.isPresent())
            return cached.map(this::cacheChatMembersToDTO);

        // загружаем информацию о чате
        Optional<Chat> chat = getChat(chatId);
        if (chat.isEmpty())
            return Optional.empty();

        // грузим из бд
        List<ChatMember> dbMembers = dbService.getChatMembers(chatId);
        if (dbMembers.isEmpty())
            return Optional.of(Collections.emptyList());

        // Сохраняем в кэш
        cacheService.addChatMembers(chat.get(), dbMembers);
        dbMembers.forEach(member ->
            cacheService.getCacheUser(member.getUserId()).ifPresent(user -> user.addChat(chatId))
        );

        return Optional.of(chatMembersToDTO(dbMembers));
    }
    public Optional<List<ChatMemberDTO>> getChatMembersPage(Long chatId, int offset, int limit) {
        // Пробуем получить страницу из кэша
        Optional<List<CacheChatMember>> cached = cacheService.getChatMembersPage(chatId, offset, limit);
        if (cached.isPresent() && !cached.get().isEmpty())
            return cached.map(this::cacheChatMembersToDTO);

        // загружаем информацию о чате
        Optional<Chat> chat = getChat(chatId);
        if (chat.isEmpty())
            return Optional.empty();

        // грузим из бд
        List<ChatMember> dbPage = dbService.getChatMembersPage(chatId, offset, limit);
        if (dbPage.isEmpty())
            return Optional.of(Collections.emptyList());

        // Сохраняем загруженную страницу в кэш
//        cacheService.addNewChatMembers(chat.get(), dbPage);

        return Optional.of(chatMembersToDTO(dbPage));
    } // TODO: НЕПРАВИЛЬНАЯ ЛОГИКА ПРОВЕРКИ КЕША (ПОТОМУ ЧТО НЕПРАВИЛЬНЫЙ ПОРЯДОК БУДЕТ)

    public Optional<Long> getChatCreator(Long chatId) {
        // грузим из бд, восстанавливаем кеш и проверяем
        return getCacheChat(chatId).map(Chat::getCreatedBy);
    }
    public Boolean hasChatMember(Long chatId, Long userId) {
        // проверка по кешу пользователя
        Optional<Boolean> userChatCheck = cacheService.getCacheUser(userId).map(user -> user.hasChat(chatId));
        if (userChatCheck.isPresent())
            return userChatCheck.get();

        // проверка через контейнер участников
        Optional<ChatMembersContainer> container = cacheService.getChatMembersContainer(chatId);
        if (container.isPresent() && container.get().hasMember(userId))
            return true;

        // загружаем информацию о чате
        Optional<Chat> chat = getChat(chatId);
        if (chat.isEmpty())
            return false;

        // проверяем пользователя в чате
        Optional<ChatMember> dbMember = dbService.getChatMember(chatId, userId);
        if (dbMember.isEmpty())
            return false;

        // кешируем
        cacheService.addChatMember(chat.get(), dbMember.get());

        return true;
    }

    private List<ChatMemberDTO> cacheChatMembersToDTO(List<CacheChatMember> cacheChatMembers){
        return cacheChatMembers.stream().flatMap(member -> {
            return getUser(member.getUserId()).map(user -> new ChatMemberDTO(member, user)).stream();
        }).toList();
    }
    private List<ChatMemberDTO> chatMembersToDTO(List<ChatMember> cacheChatMembers){
        return cacheChatMembers.stream().flatMap(member -> {
            return getUser(member.getUserId()).map(user -> new ChatMemberDTO(member, user)).stream();
        }).toList();
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(VerificationToken verifToken) {
        cacheService.saveVerificationToken(verifToken); // сохраняем в кеш
        dbService.saveVerificationTokenAsync(verifToken); // асинхронно в бд
    }
    public void deleteVerificationToken(String token) {
        cacheService.deleteVerificationToken(token); // сохраняем в кеш
        dbService.deleteVerificationTokenAsync(token); // асинхронно в бд
    }


    // Вспомогательные методы
    public Optional<VerificationToken> getVerificationToken(String token) {
        Optional<VerificationToken> optToken = cacheService.getVerificationToken(token);
        if(optToken.isPresent())
            return optToken;

        Optional<VerificationToken> optTokenDB = dbService.getVerificationToken(token);
        log.debug("[🏛️] Token {} loaded || getVerificationToken", token);
        optTokenDB.ifPresent(vrfToken -> {
            cacheService.saveVerificationToken(vrfToken);
            log.debug("[⚡] Token {} loaded || getVerificationToken", token);
        });
        return cacheService.getVerificationToken(token);
    }
    public int cleanupExpiredTokensFromDB() {
        return dbService.cleanupExpiredVerificationTokens();  // синхронно из бд
    }

    // ========== MESSAGE METHODS ==========

    public void saveMessage(Message message) {
        dbService.saveMessageAsync(message);
    }

    public List<MessageDBResult> getChatMessagesFirst(Long chatId, Long userId, Integer limit) {
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


    // ========== DTO METHODS ==========




    // ========== CACHE METHODS ==========
    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }

    @Scheduled(fixedDelay = 3600000) // Каждые 1 час
    public void logCacheStats() {

        var cacheStats = cacheService.getDetailedCacheStats();

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
                cacheStats.get("chatMemberCache.estimatedSize"),
                Math.round((Double)cacheStats.get("chatMemberCache.hitRate") * 100),
                Math.round((Double)cacheStats.get("chatMemberCache.missRate") * 100),
                cacheStats.get("chatMemberCache.evictionCount"));

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


    // ========== SUB METHODS ==========

    public static Long randomId() {
        return Math.abs(new SecureRandom().nextLong());
    }
    public static String generate64CharString() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[48]; // 48 bytes = 64 base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}