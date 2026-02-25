package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.*;
import com.Sunrise.DTO.Responses.ChatDTO;
import com.Sunrise.DTO.Responses.ChatMemberDTO;
import com.Sunrise.Entities.Cache.CacheUser;
import com.Sunrise.Entities.Cache.ChatMembersContainer;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

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
    public void saveUser(User user) {
        cacheService.saveUser(user); // сохраняем в кеш

        // инвалидируем кеш поиска
        cacheService.invalidateUsersPagination();

        dbService.saveUserAsync(user); // асинхронно в бд
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
    public Optional<User> getUser(Long userId) {
        // пробуем кеш
        Optional<CacheUser> cached = cacheService.getCacheUser(userId);
        if (cached.isPresent())
            return cached.map(User::new);

        // грузим из бд
        Optional<User> dbUser = dbService.getUser(userId);
        log.debug("[🏛️] Loaded user {} || getUser", userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(user); // сохраняем в кеш
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
            cacheService.saveUser(user); // сохраняем в кеш
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
            cacheService.saveUser(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || getUserByEmail", user.getId());
        });
        return dbUser;
    }

    private List<User> getUsersByIds(List<Long> userIds) {
        if (userIds.isEmpty())
            return Collections.emptyList();

        Map<Long, User> userMap = new HashMap<>();
        List<Long> missingIds = new ArrayList<>(userIds.size() / 2);

        // получаем из кеша
        for (Long id : userIds) {
            Optional<CacheUser> cachedUser = cacheService.getCacheUser(id);
            if (cachedUser.isPresent()) {
                userMap.put(id, new User(cachedUser.get()));
            } else {
                missingIds.add(id);
            }
        }

        // получаем из бд
        if (!missingIds.isEmpty()) {
            List<User> dbUsers = dbService.getUsersByIds(missingIds);
            for (User user : dbUsers) {
                cacheService.saveUser(user); // кешируем
                userMap.put(user.getId(), user);
            }
            log.debug("[🏛️] Loaded {} missing users from DB: {} || getUsersByIds", missingIds.size(), missingIds);
        }

        // сортируем
        return userIds.stream().map(userMap::get).filter(Objects::nonNull).toList();
    }
    public List<User> getFilteredUsersPage(String filter, int offset, int limit) {
        // Пробуем найти в кеше
        Optional<CacheService.UsersPagination> cached = cacheService.findUsersPagination(filter, offset, limit);
        if (cached.isPresent()) {
            CacheService.UsersPagination pagination = cached.get();
            List<User> users = getUsersByIds(pagination.userIds());
            log.debug("[⚡] Cache hit for users page filter='{}' {}/{}", filter, offset, limit);
            return users;
        }

        log.debug("[🏛️] Loading users page filter='{}' {}/{} from DB", filter, offset, limit);

        // получаем пагинацию и сохраняем в кеш
        UsersPageResult pageResult = dbService.getFilteredUsersPage(filter, offset, limit);

        log.debug("[🏛️] Loading users page {} from DB", pageResult.getUserIds());
        List<Long> userIds = pageResult.getUserIds();
        cacheService.saveUsersPagination(
            CacheService.UsersPagination.builder()
                .id(randomId())
                .filter(filter)
                .offset(offset)
                .limit(limit)
                .userIds(userIds)
                .createdAt(LocalDateTime.now())
                .hasMore(pageResult.getHasMore())
                .totalCount(pageResult.getTotalCount())
                .build()
        );

        // загружаем пользователей по ID
        return getUsersByIds(userIds);
    }

    public boolean existsUser(Long userId) {
        // проверяем в кеше
        if (cacheService.existsUser(userId))
            return true;

        // проверяем в бд
        Optional<User> dbUser = dbService.getUser(userId);
        log.debug("[🏛️] Loaded user {} || existsUserById", userId);
        dbUser.ifPresent(user -> {
            cacheService.saveUser(user); // сохраняем в кеш
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
            cacheService.saveUser(user); // сохраняем в кеш
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
            cacheService.saveUser(user); // сохраняем в кеш
            log.debug("[⚡] Loaded user {} || existsUserByEmail", user.getId());
        });
        return dbUser.isPresent();
    }


    // Методы для кеша
    private void loadFullUserToCache(User user){
        cacheService.saveUser(user); // сохраняем в кеш
        List<Long> dbChatIds = dbService.getUserChatIds(user.getId()); // получаем id чатов
        log.debug("[🏛️] Loaded {} chatsIds for user {} || loadUserToCache", dbChatIds.size(), user.getId());
        cacheService.setUserChatsIds(user.getId(), new HashSet<>(dbChatIds), true); // затем сохраняем его чаты (только их id)
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
        cacheService.invalidateAfterChatAdded(new Long[] {creator.getUserId(), member.getUserId()});
        log.debug("[⚡] Invalidating pagination cache for users --> {}, {} | savePersonalChatAndAddPerson", creator.getUserId(), member.getUserId());

        // асинхронно в бд
        dbService.savePersonalChatAsync(chat.getId(), creator.getUserId(), member.getUserId());
    }
    public void saveGroupChatAndAddPeople(Chat chat, List<ChatMember> members) {
        cacheService.saveNewGroupChat(chat, members); // сохраняем в кеш

        // Инвалидируем пагинацию
        Long[] membersIds = members.stream().map(ChatMember::getUserId).toArray(Long[]::new);
        cacheService.invalidateAfterChatAdded(membersIds);
        log.debug("[⚡] Invalidated pagination cache for {} users | saveGroupChatAndAddPeople", membersIds.length);

        // асинхронно в бд
        dbService.saveGroupChatAsync(chat, membersIds);
    }
    public void restoreChat(Long chatId) {
        // Получаем всех участников чата до восстановления
        List<Long> membersIds = dbService.getChatMemberIds(chatId);

        cacheService.restoreChat(chatId); // сохраняем в кеш

        // Инвалидируем пагинацию
        cacheService.invalidateAfterChatRestored(membersIds);
        log.debug("[⚡] Invalidated pagination cache for {} users | restoreChat", membersIds.size());

        dbService.restoreChatAsync(chatId); // асинхронно в бд
    }
    public void deleteChat(Long chatId) {
        // Получаем всех участников чата до удаления
        List<Long> membersIds = dbService.getChatMemberIds(chatId);

        cacheService.deleteChat(chatId); // сохраняем в кеш

        // Инвалидируем пагинацию для всех участников
        cacheService.invalidateAfterChatDeleted(membersIds);
        log.debug("[⚡] Invalidated pagination cache for {} users | deleteChat", membersIds.size());

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
            return !loadChatToCache(chat).isDeleted(); // восстанавливаем в кеш
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
        log.debug("[🏛️] Loaded {} chat {} || reloadChatCache", chat.isGroup() ? "group" : "personal", chat.getId());
        return Optional.of(loadChatToCache(chat));
    }

    public List<ChatDTO> getUserChats(Long userId) {
        // есть ВСЕ chatIds в кеше, подгружаем НЕКОТОРЫЕ чаты, если их нет
        List<ChatDTO> result = new ArrayList<>();
        Set<Long> chatIds;
        Optional<AbstractMap.SimpleEntry<Set<Long>, Boolean>> cachedChatIds = cacheService.getUserChatsIds(userId);

        if (cachedChatIds.isPresent()) {
            // ищем чаты, которые надо подгрузить с бд
            chatIds = cachedChatIds.get().getKey();
            boolean isFullyLoaded = cachedChatIds.get().getValue();

            if (!isFullyLoaded) {

                List<Long> missingChatIds = dbService.getMisingUserChatIds(userId, chatIds);
                if (!missingChatIds.isEmpty()) {
                    cacheService.addUserChatsBatch(userId, new HashSet<>(missingChatIds), true);
                    chatIds.addAll(missingChatIds);
                    log.debug("[⚡] Loaded additional {} chats for user {}", missingChatIds.size(), userId);
                }
            }

            List<Long> missingChatIds = new ArrayList<>();
            for (Long chatId : chatIds) {
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

            return result;
        }

        // НЕТ chatIds в кеше, подгружаем ВСЕ чаты из бд

        List<Chat> userChats = dbService.getUserChats(userId);
        chatIds = new HashSet<>(userChats.size());
        if (!userChats.isEmpty()) {
            log.debug("[🏛️] Loaded {} missing chat(s) with members for user {} || getUserChats", userChats.size(), userId);
            userChats.forEach(chat -> {
                loadChatToCache(chat);
                result.add(new ChatDTO(chat));
                chatIds.add(chat.getId());
            });
            cacheService.setUserChatsIds(userId, chatIds, true);
        }

        return result;
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
                Chat chat = cachedChat.get();
                chatMap.put(chat.getId(), new ChatDTO(chat));
            } else {
                missingIds.add(chatId);
            }
        }

        // получаем из бд
        if (!missingIds.isEmpty()) {
            List<Chat> dbChats = dbService.getChatsByIds(missingIds);
            for (Chat chat : dbChats) {
                cacheService.saveExistingChat(chat); // кешируем
                chatMap.put(chat.getId(), new ChatDTO(chat));
            }
            log.debug("[🏛️] Loaded {} missing chats from DB: {}", missingIds.size(), missingIds);
        }

        // сортируем
        return chatIds.stream().map(chatMap::get).filter(Objects::nonNull).toList();
    }
    public List<ChatDTO> getUserChatsPage(Long userId, int offset, int limit) {
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

    public Optional<Boolean> isGroupChat(Long chatId) {
        // пробуем кеш
        Optional<Boolean> isGroup = cacheService.getIsGroupChat(chatId);
        if (isGroup.isPresent())
            return isGroup;

        // грузим из бд
        Optional<Chat> dbChat = dbService.getChat(chatId);
        log.debug("[🏛️] Chat {} loaded || isGroupChat", chatId);
        return dbChat.map(chat -> loadChatToCache(chat).isGroup()); // восстанавливаем кеш
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
            return member.isAdmin();
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
        Optional<ChatMember> dbMember = dbService.findAnotherChatAdmin(chatId, excludeUserId);
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
    private CacheChat loadChatToCache(Chat chat) {
        var cacheChat = cacheService.saveExistingChat(chat); // сохраняем чат в кеш
        log.debug("[⚡] Loaded {} chat {} || loadChatToCache", cacheChat.isGroup() ? "group" : "personal", cacheChat.getId());
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

        // ИНВАЛИДАЦИЯ
        cacheService.invalidateAfterMemberAdded(chatMember.getChatId(), chatMember.getUserId());
        log.debug("[⚡] Invalidated pagination cache for user {} | saveChatMember", chatMember.getUserId());

        dbService.upsertChatMemberAsync(chatMember); // асинхронно в бд
    }
    public void updateChatCreator(Long chatId, Long newCreatorId) {
        cacheService.updateChatCreator(chatId, newCreatorId); // сохраняем в кеш
        dbService.updateChatCreatorAsync(chatId, newCreatorId); // асинхронно в бд
    }
    public void updateAdminRights(Long chatId, Long userId, Boolean isAdmin) {
        cacheService.saveAdminRights(chatId, userId, isAdmin); // обновляем кэш
        dbService.updateUserAdminRightsAsync(chatId, userId, isAdmin); // асинхронно в бд
    }
    public void removeUserFromChat(Long chatId, Long userId) {
        cacheService.removeChatMember(userId, chatId); // сохраняем в кеш

        // ИНВАЛИДАЦИЯ
        cacheService.invalidateAfterMemberRemoved(chatId, userId);
        log.debug("[⚡] Invalidated pagination cache for user {} | removeUserFromChat", userId);

        dbService.removeUserFromChatAsync(userId, chatId); // асинхронно в бд
    }


    // Вспомогательные методы
    private List<ChatMemberDTO> getChatMembersBatch(List<Long> userIds, Long chatId) {
        if (userIds.isEmpty()) return Collections.emptyList();

        Optional<Chat> optChat = getChat(chatId);
        if (optChat.isEmpty()) return Collections.emptyList();

        List<Long> missingUserIds = new ArrayList<>();
        List<Long> missingMemberIds = new ArrayList<>();

        Map<Long, User> userMap = new HashMap<>(userIds.size());
        Map<Long, CacheChatMember> memberMap = new HashMap<>(userIds.size());

        // получаем из кеша User и ChatMember, если есть
        for (Long userId : userIds) {
            // User
            Optional<CacheUser> cachedUser = cacheService.getCacheUser(userId);
            if (cachedUser.isPresent()) {
                userMap.put(userId, new User(cachedUser.get()));
            } else {
                missingUserIds.add(userId);
            }

            // ChatMember
            Optional<CacheChatMember> cachedMember = cacheService.getChatMember(chatId, userId);
            if (cachedMember.isPresent()) {
                memberMap.put(userId, cachedMember.get());
            } else {
                missingMemberIds.add(userId);
            }
        }

        // получаем User из бд
        if (!missingUserIds.isEmpty()) {
            List<User> dbUsers = dbService.getUsersByIds(missingUserIds);
            for (User user : dbUsers) {
                cacheService.saveUser(user); // Кешируем
                userMap.put(user.getId(), user);
            }
        }

        // получаем ChatMember из бд
        if (!missingMemberIds.isEmpty()) {
            List<ChatMember> dbMembers = dbService.getChatMembersByIds(chatId, missingMemberIds);
            Chat chat = optChat.get();
            for (ChatMember member : dbMembers) {
                cacheService.addChatMember(chat, member); // Кешируем
                memberMap.put(member.getUserId(), new CacheChatMember(member));
            }
        }

        // Формируем результат
        List<ChatMemberDTO> result = new ArrayList<>(userIds.size());
        for (Long userId : userIds) {
            User user = userMap.get(userId);
            CacheChatMember member = memberMap.get(userId);
            if (user != null && member != null) {
                result.add(new ChatMemberDTO(member, user));
            }
        }

        return result;
    }
    public List<ChatMemberDTO> getChatMembersPage(Long chatId, int offset, int limit) {
        // пробуем кеш
        Optional<CacheService.ChatMembersPagination> pagination = cacheService.findChatMembersPagination(chatId, offset, limit);
        if (pagination.isPresent()) {
            List<ChatMemberDTO> members = getChatMembersBatch(pagination.get().memberUserIds(), chatId);
            log.debug("[⚡] Cache hit for chat {} members page {}/{}", chatId, offset, limit);
            return members;
        }

        log.debug("[🏛️] Loading chat {} members page {}/{} from DB || getChatMembersPage", chatId, offset, limit);

        // загружаем с бд и сохраняем пагинацию
        ChatMembersPageResult pageResult = dbService.getChatMembersPage(chatId, offset, limit);
        cacheService.saveChatMembersPagination(
            CacheService.ChatMembersPagination.builder()
                    .id(randomId())
                    .chatId(chatId)
                    .offset(offset)
                    .limit(limit)
                    .memberUserIds(pageResult.getUserIds())
                    .createdAt(LocalDateTime.now())
                    .hasMore(pageResult.getHasMore())
                    .totalCount(pageResult.getTotalCount())
                    .build()
        );

        // загружаем также с бд
        return getChatMembersBatch(pageResult.getUserIds(), chatId);
    }

    public Optional<Long> getChatCreator(Long chatId) {
        // грузим из бд, восстанавливаем кеш и проверяем
        return getCacheChat(chatId).map(Chat::getCreatedBy);
    }
    public Boolean hasChatMember(Long chatId, Long userId) {
        // проверка по кешу пользователя
        Optional<Boolean> userChatCheck = cacheService.getCacheUser(userId).map(user -> user.hasChat(chatId));
        if (userChatCheck.isPresent() && userChatCheck.get().equals(true))
            return true;

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


    // ========== CACHE METHODS ==========


    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }
    public void printCacheStatus() {
        cacheService.printCacheStats();
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