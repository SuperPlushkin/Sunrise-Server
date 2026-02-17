package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.DTO.DBResults.MessageDBResult;
import com.Sunrise.Entities.Cache.CacheUser;
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
        dbService.saveUserAsync(user); // асинхронно в бд
    }
    public void enableUser(Long userId) {
        cacheService.updateUserIsEnabled(userId, true); // сохраняем в кеш
        dbService.enableUserAsync(userId); // асинхронно в бд
    }
    public void deleteUser(Long userId) {
        cacheService.deleteUser(userId); // сохраняем в кеш
        dbService.deleteUserAsync(userId); // асинхронно в бд
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
    public List<User> getFilteredUsers(String filter, int limit, int offset) {
        // пробуем кеш
        Optional<List<User>> cached = cacheService.getUsersSearchResult(filter, limit, offset);
        if (cached.isPresent())
            return cached.get();

        // грузим из бд
        List<User> dbResults = dbService.getFilteredUsers(filter, limit, offset);
        log.debug("[🏛️] {} users search result loaded || getFilteredUsers", dbResults.size());
        if (!dbResults.isEmpty()) {
            log.debug("[⚡] Users search result loaded with {} users || getFilteredUsers", dbResults.size());
            cacheService.saveUsersSearchResult(filter, limit, offset, dbResults); // кешируем результат
        }
        return dbResults;
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
    public void saveLoginHistory(Long userId, String ipAddress, String deviceInfo) {
        LoginHistory loginHistory = new LoginHistory(generateRandomId(), userId, ipAddress, deviceInfo, LocalDateTime.now());
        dbService.saveLoginHistoryAsync(loginHistory); // асинхронно в бд
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void savePersonalChatAndAddPerson(Chat chat, ChatMember creator, ChatMember member) {
        cacheService.savePersonalChat(chat, creator, member); // сохраняем в кеш

        // асинхронно в бд
        dbService.saveChatAsync(chat);
        dbService.upsertChatMemberAsync(creator);
        dbService.upsertChatMemberAsync(member);
    }
    public void saveGroupChatAndAddPeople(Chat chat, List<ChatMember> members) {
        cacheService.saveGroupChat(chat, members); // сохраняем в кеш

        // асинхронно в бд
        dbService.saveChatAsync(chat);
        members.forEach(dbService::upsertChatMemberAsync);
    }
    public void restoreChat(Long chatId) {
        cacheService.restoreChat(chatId); // сохраняем в кеш
        dbService.restoreChatAsync(chatId); // асинхронно в бд
    }
    public void deleteChat(Long chatId) {
        cacheService.deleteChat(chatId); // сохраняем в кеш
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
        return dbChat.map(chat ->{
            loadChatToCache(chat); // восстанавливаем в кеш
            return !chat.getIsDeleted();
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

    public Optional<Long> getPersonalChatId(Long userId1, Long userId2) {
        return getPersonalChatIdByIsDeleted(userId1, userId2, false);
    }
    public Optional<Long> getDeletedPersonalChatId(Long userId1, Long userId2) {
        return getPersonalChatIdByIsDeleted(userId1, userId2, true);
    }
    private Optional<Long> getPersonalChatIdByIsDeleted(Long userId1, Long userId2, boolean deleted) {
        // пробуем кеш
        Optional<Long> cached = cacheService.findPersonalChatByIsDeleted(userId1, userId2, deleted);
        if (cached.isPresent())
            return cached;

        // грузим из бд
        Optional<Long> dbChatId = deleted ?
                dbService.findDeletedPersonalChat(userId1, userId2) :
                dbService.findPersonalChat(userId1, userId2);

        dbChatId.ifPresent(this::reloadFullChatCache);

        return dbChatId;
    } // TODO: НЕ ОПТИМАЛЬНО

    public Optional<List<CacheChat>> getUserChats(Long userId) {
        // проверяем что пользователь существует
        if (!existsUser(userId))
            return Optional.empty();

        // есть ВСЕ chatIds в кеше, подгружаем НЕКОТОРЫЕ чаты, если их нет
        List<CacheChat> result = new ArrayList<>();
        Optional<Set<Long>> cachedChatIds = cacheService.getUserChatsIds(userId);
        if (cachedChatIds.isPresent()) {
            // ищем чаты, которые надо подгрузить с бд
            Set<Long> missingChatIds = new HashSet<>();
            for (Long chatId : cachedChatIds.get()) {
                Optional<CacheChat> cachedChat = cacheService.getChatCache(chatId);
                if (cachedChat.isPresent()) {
                    result.add(cachedChat.get());
                } else {
                    missingChatIds.add(chatId);
                }
            }

            // Загружаем недостающие чаты из БД
            if (!missingChatIds.isEmpty()) {
                List<Chat> dbChats = dbService.getChatsByIds(missingChatIds);
                log.debug("[🏛️] Loaded {} missing chat(s) with members for user {} || getUserChats", missingChatIds.size(), userId);
                dbChats.forEach(chat -> result.add(loadChatToCache(chat)));
            }

            return Optional.of(result);
        }

        // НЕТ chatIds в кеше, подгружаем ВСЕ чаты из бд
        List<Chat> userChats = dbService.getUserChats(userId);
        if (!userChats.isEmpty()) {
            log.debug("[🏛️] Loaded {} missing chat(s) with members for user {} || getUserChats", userChats.size(), userId);
            userChats.forEach(chat -> result.add(loadChatToCache(chat)));
        }

        return Optional.of(result);
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

        // грузим из бд, отправляем в кеш, проверяем
        return reloadFullChatCache(chatId).map(chat -> chat.isMemberAdmin(userId));
    }
    public Optional<Long> findAnotherAdmin(Long chatId, Long excludeUserId) {
        // пробуем кеш
        Optional<Set<Long>> adminsOpt = cacheService.getChatAdmins(chatId);
        if (adminsOpt.isPresent())
            return adminsOpt.get().stream().filter(adminId -> !adminId.equals(excludeUserId)).findFirst();

        // грузим из бд, отправляем в кеш, проверяем
        return reloadFullChatCache(chatId).map(chat -> chat.getOtherMemberAdminId(excludeUserId));
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
    private CacheChat loadChatToCache(Chat chat){
        var cacheChat = cacheService.saveChat(chat); // сохраняем чат в кеш
        log.debug("[⚡] Loaded {} chat {} || loadChatToCache", cacheChat.getIsGroup() ? "group" : "personal", cacheChat.getId());
        return cacheChat;
    }
    private CacheChat loadFullChatToCache(Chat chat){
        var cacheChat = cacheService.saveChat(chat); // сначала сохраняем чат в кеш
        reloadChatMembersToCache(cacheChat); // потом получаем и сохраняем участников в кеш
        if (cacheChat.isPersonalChat()) {
            Long creatorId = cacheChat.getCreatedBy();
            Long otherId = cacheChat.getOtherMemberId(creatorId);
            if (otherId != null)
                cacheService.savePersonalChatIndex(cacheChat.getId(), creatorId, otherId); // сохраняем в кеш (для индекса по isPersonalChat)
        }

        log.debug("[⚡] Loaded {} chat {} || loadFullChatToCache", cacheChat.getIsGroup() ? "group" : "personal", cacheChat.getId());
        return cacheChat;
    }
    private Optional<CacheChat> reloadChatCache(Long chatId) {
        Optional<Chat> dbChat = dbService.getChat(chatId);
        if (dbChat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || reloadChatCache", chatId);
            return Optional.empty();
        }

        Chat chat = dbChat.get();
        log.debug("[🏛️] Loaded {} chat {} || reloadChatCache", chat.getIsGroup() ? "group" : "personal", chat.getId());
        return Optional.of(loadChatToCache(chat));
    }
    private Optional<CacheChat> reloadFullChatCache(Long chatId) {
        Optional<Chat> dbChat = dbService.getChat(chatId);
        if (dbChat.isEmpty()) {
            log.warn("[🏛️] Chat {} not found || reloadFullChatCache", chatId);
            return Optional.empty();
        }

        Chat chat = dbChat.get();
        log.debug("[🏛️] Loaded {} chat {} || reloadFullChatCache", chat.getIsGroup() ? "group" : "personal", chat.getId());
        return Optional.of(loadFullChatToCache(chat));
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void saveChatMember(ChatMember chatMember) {
        cacheService.saveChatMember(chatMember); // сохраняем в кеш
        dbService.upsertChatMemberAsync(chatMember); // асинхронно в бд
    }
    public void updateChatCreator(Long chatId, Long newCreatorId) {
        // сохраняем в кеш
        cacheService.getChatCache(chatId).ifPresent(cacheChat -> {
            cacheChat.setCreatedBy(newCreatorId);
            cacheChat.setAdminRights(newCreatorId, true);
        });

        // асинхронно в бд
        dbService.updateChatCreatorAsync(chatId, newCreatorId);
    }
    public void removeUserFromChat(Long userId, Long chatId) {
        cacheService.removeChatMember(userId, chatId); // сохраняем в кеш
        dbService.removeUserFromChatAsync(userId, chatId); // асинхронно в бд
    }


    // Вспомогательные методы
    public Optional<List<CacheChatMember>> getChatMembers(Long chatId) {
        // пробуем кеш
        Optional<List<CacheChatMember>> cached = cacheService.getChatMembers(chatId);
        if (cached.isPresent())
            return cached;

        // грузим из бд, восстанавливаем кеш и проверяем
        return reloadFullChatCache(chatId).map(CacheChat::getMembers).map(CacheChatMember::new);
    }
    public Optional<Long> getChatCreator(Long chatId) {
        // пробуем кеш
        Optional<Long> cached = cacheService.getChatCreator(chatId);
        if (cached.isPresent())
            return cached;

        // грузим из бд, восстанавливаем кеш и проверяем
        return reloadChatCache(chatId).map(Chat::getCreatedBy); // восстанавливаем в кеш
    }
    public Boolean hasChatMember(Long chatId, Long userId) {
        // пробуем кеш
        Optional<Boolean> result = cacheService.isUserInChat(chatId, userId);
        if (result.isPresent())
            return result.get();

        // грузим из бд, восстанавливаем кеш и проверяем
        return reloadFullChatCache(chatId).map(chat -> chat.hasNotDeletedMember(userId)).orElse(false);
    }


    // Методы для кеша
    private List<ChatMember> reloadChatMembersToCache(CacheChat chat) {
        // загружаем участников из бд
        List<ChatMember> dbMembers = dbService.getChatMembers(chat.getId());
        if (dbMembers.isEmpty()) {
            cacheService.clearChatMembers(chat.getId()); // нет участников - сохраняем пустой список
            log.debug("[⚡] Chat {} has no members || loadChatMembersToCache", chat.getId());
            return dbMembers;
        }

        // сохраняем участников в кеш
//        dbMembers.forEach(member -> getUser(member.getUserId()));
        dbMembers.forEach(chat::addMember);

        log.debug("[⚡] Loaded {} members for chat {} || loadChatMembersToCache", dbMembers.size(), chat.getId());
        return dbMembers;
    } // TODO: ЕСЛИ НИЧО НЕ РАБОТАЕТ НАДО РАСКОММЕНТИТЬ


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


    // ========== CACHE METHODS ==========
    public CacheService.CacheStats getCacheStatus() {
        return cacheService.getCacheStatus();
    }

    @Scheduled(fixedDelay = 90000) // Каждые 1.5 минуты
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

    public static Long generateRandomId() {
        SecureRandom random = new SecureRandom();
        return Math.abs(random.nextLong());
    }
    public static String generate64CharString() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[48]; // 48 bytes = 64 base64 characters
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}