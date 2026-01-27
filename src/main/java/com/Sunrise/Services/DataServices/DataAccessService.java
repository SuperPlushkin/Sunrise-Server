package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsResult;
import com.Sunrise.DTO.DBResults.GetChatMemberResult;
import com.Sunrise.DTO.DBResults.GetPersonalChatResult;
import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.DTO.ServiceResults.UserDTO;
import com.Sunrise.Entities.Chat;
import com.Sunrise.Entities.LoginHistory;
import com.Sunrise.Entities.User;
import com.Sunrise.Entities.VerificationToken;
import com.Sunrise.Services.DataServices.CacheEntities.CacheChat;
import com.Sunrise.Services.DataServices.Interfaces.IAsyncStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DataAccessService {

    private static final Logger log = LoggerFactory.getLogger(DataAccessService.class);
    private final CacheService cacheService;
    private final IAsyncStorageService dbService;

    public DataAccessService(CacheService cacheService, IAsyncStorageService  dbService) {
        this.cacheService = cacheService;
        this.dbService = dbService;
    }


    // ========== CACHE INITIALIZATION METHODS ==========

    @PostConstruct
    public void initializeFullCache() {
        log.info("------------------------------------------------------");
        log.info("🔄 Starting full cache initialization from database...");

        try {
            long startTime = System.currentTimeMillis();

            loadAllUsersToCache(); // 1. Загружаем ВСЕХ пользователей

            loadAllChatsToCache(); // 2. Загружаем ВСЕ чаты

            loadActiveVerificationTokensToCache(); // 3. Загружаем ВСЕ токены подтверждения

            initializeChatMemberships(); // 4. Инициализируем ВСЕ связи пользователей с чатами

            initializePersonalChats(); // 5. Инициализируем ВСЕ личные чаты

            long endTime = System.currentTimeMillis();

            printInitializationStats(endTime - startTime);

        } catch (Exception e) {
            log.error("❌ Cache initialization failed: {}", e.getMessage());
        }

        log.info("------------------------------------------------------");
    }

    private void loadAllUsersToCache() {
        for (User user : dbService.getAllUsers()) {
            cacheService.saveUser(user);
        }
    }
    private void loadAllChatsToCache() {
        for (Chat chat : dbService.getAllChats()) {
            cacheService.saveChat(chat);
        }
    }
    private void loadActiveVerificationTokensToCache() {
        for (VerificationToken token : dbService.getAllVerificationTokens()) {
            cacheService.saveVerificationToken(token);
        }
    }
    private void initializeChatMemberships() {
        for (GetChatMemberResult membership : dbService.getAllChatMembers()) { // Загружаем все членства в чатах
            Long chatId = membership.getChatId();
            Long userId = membership.getUserId();
            Boolean isAdmin = membership.getIsAdmin();

            cacheService.addUserToChatWith(chatId, userId, isAdmin); // Добавляем пользователя в чат с правами администратора
        }
    }
    private void initializePersonalChats() {
        for (GetPersonalChatResult personalChat : dbService.getAllPersonalChats()) {
            Long chatId = personalChat.getChatId();
            Long userId1 = personalChat.getUserId1();
            Long userId2 = personalChat.getUserId2();

            cacheService.savePersonalChat(userId1, userId2, chatId);
        }
    }

    private void printInitializationStats(long duration) {
        CacheService.CacheStats stats = cacheService.getStats();
        log.info("✅ Full cache initialization completed in {} ms", duration);
        log.info("📊 Final Cache Statistics:");
        log.info("   ├─ Active Users: {}", stats.activeUserCount());
        log.info("   ├─ Activated Users: {}", stats.activatedUserCount());
        log.info("   ├─ Users: {}", stats.userCount());
        log.info("   ├─ Active Chats: {}", stats.chatCount());
        log.info("   ├─ Active Sessions: {}", stats.activeUserCount());
        log.info("   ├─ Verification Tokens: {}", stats.verificationTokenCount());
        log.info("   ├─ User-Chat Relations: {}", stats.userChatsCount());
        log.info("   ├─ Chat Members: {}", stats.chatMembersCount());
        log.info("   └─ Admin Rights: {}", stats.adminRightsCount());
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(User user) {
        cacheService.saveUser(user);
        dbService.saveUserAsync(user);
    }
    public void enableUser(Long userId) {
        cacheService.enableUser(userId);
        dbService.enableUserAsync(userId);
    }
    public void deleteUser(Long userId) {
        cacheService.deleteUser(userId);
        dbService.deleteUserAsync(userId);
    }


    // Вспомогательные методы
    public Optional<User> getUser(Long userId) {
        return cacheService.getUser(userId);
    }
    public Optional<User> getUserByUsername(String username) {
        return cacheService.getUserByUsername(username);
    }
    public List<UserDTO> getFilteredUsers(String filter, int limit, int offset) {
        return cacheService.getFilteredUsers(filter, limit, offset).stream().map(UserDTO::new).toList();
    }
    public boolean notExistsUserById(Long userId) {
        return !cacheService.existsUser(userId);
    }
    public Boolean existsUserByUsername(String username) {
        return cacheService.existsUserByUsername(username);
    }
    public Boolean existsUserByEmail(String email) {
        return cacheService.existsUserByEmail(email);
    }
    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        cacheService.updateLastLogin(username, lastLogin);
        dbService.updateLastLoginAsync(username, lastLogin);
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    public void saveLoginHistory(Long userId, String ipAddress, String deviceInfo) {
        LoginHistory loginHistory = new LoginHistory(generateRandomId(), userId, ipAddress, deviceInfo, LocalDateTime.now());

        dbService.saveLoginHistoryAsync(loginHistory);
    }  // БЕЗ КЭША


    // ========== CHAT METHODS ==========


    // Основные методы
    public void savePersonalChatAndAddPerson(Chat chat, Long userToAdd) {
        cacheService.savePersonalChat(chat, userToAdd);

        dbService.saveChatAsync(chat);
        dbService.addUserToChatAsync(userToAdd, chat.getId(), true);
        dbService.addUserToChatAsync(userToAdd, chat.getId(), true);
    }
    public void saveGroupChatAndAddPeople(Chat chat, Set<Long> usersId) {
        cacheService.saveGroupChat(chat, usersId);

        dbService.saveChatAsync(chat);
        dbService.addUserToChatAsync(chat.getCreatedBy(), chat.getId(), true);
        for (Long userId : usersId)
            dbService.addUserToChatAsync(userId, chat.getId(), false);
    }
    public void deleteChat(Long chatId) {
        cacheService.deleteChat(chatId);
        dbService.deleteChatAsync(chatId);
    }


    // Вспомогательные методы
    public Optional<Long> findPersonalChat(Long userId1, Long userId2) {
        return cacheService.findExistingPersonalChat(userId1, userId2);
    }
    public Optional<Boolean> isGroupChat(Long chatId) {
        return cacheService.isGroupChat(chatId);
    }
    public Boolean existsChat(Long chatId) {
        return cacheService.existsChat(chatId);
    }
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        return cacheService.isChatAdmin(chatId, userId);
    }
    public Optional<Long> findAnotherAdmin(Long chatId, Long excludeUserId) {
        Set<Long> admins = cacheService.getChatAdmins(chatId);
        if (!admins.isEmpty()) {
            for (Long adminId : admins) {
                if (!adminId.equals(excludeUserId))
                    return Optional.of(adminId);
            }
        }

        return Optional.empty();
    }
    public Integer getChatMemberCount(Long chatId) {
        return cacheService.getChatMembers(chatId).size();
    }


    // Методы для работы с сообщениями (пока что все с бд)
    public List<MessageResult> getChatMessages(Long chatId, Long userId, Integer limit, Integer offset) {
        return dbService.getChatMessages(chatId, userId, limit, offset);
    }
    public Integer getVisibleMessagesCount(Long chatId, Long userId) {
        return dbService.getVisibleMessagesCount(chatId, userId); // добавить кеш можно будет
    }
    public void markMessageAsRead(Long messageId, Long userId) {
        dbService.markMessageAsRead(messageId, userId);
    }


    // Методы для истории чатов (пока что все с бд)
    public Integer clearChatHistoryForAll(Long chatId, Long userId) {
        return dbService.clearChatHistoryForAll(chatId, userId);
    }
    public Integer clearChatHistoryForSelf(Long chatId, Long userId) {
        return dbService.clearChatHistoryForSelf(chatId, userId);
    }
    public Integer restoreChatHistoryForSelf(Long chatId, Long userId) {
        return dbService.restoreChatHistoryForSelf(chatId, userId);
    }
    public ChatStatsResult getChatClearStats(Long chatId, Long userId) {
        return dbService.getChatClearStats(chatId, userId);
    }


    // ========== CHAT MEMBER METHODS ==========

    public Set<Long> getChatMembers(Long chatId) {
        return cacheService.getChatMembers(chatId);
    }
    public Optional<List<Chat>> getUserChats(Long userId) {
        Optional<List<Long>> cachedChatIds = cacheService.getUserChats(userId);
        List<Chat> result = null;

        if (cachedChatIds.isPresent() && !cachedChatIds.get().isEmpty()) {
            result = new ArrayList<>();
            for (Long chatId : cachedChatIds.get()) {
                Optional<CacheChat> cacheChat = cacheService.getChatInfo(chatId);
                cacheChat.ifPresent(result::add);
            }
        }

        return Optional.ofNullable(result);
    }
    public Boolean isUserInChat(Long chatId, Long userId) {
        return cacheService.isUserInChat(chatId, userId);
    }
    public void addUserToChat(Long userId, Long chatId, Boolean isAdmin) {
        cacheService.addUserToChatWith(chatId, userId, isAdmin);
        dbService.addUserToChatAsync(userId, chatId, isAdmin);
    }
    public void removeUserFromChat(Long userId, Long chatId) {
        cacheService.removeUserFromChat(userId, chatId);
        dbService.removeUserFromChatAsync(userId, chatId);
    }

    public Optional<Long> getChatCreator(Long chatId) {
        return cacheService.getChatCreator(chatId);
    }
    public void updateChatCreator(Long chatId, Long newCreatorId) {
        cacheService.getChatInfo(chatId).ifPresent(cacheChat -> {
            cacheChat.setCreatedBy(newCreatorId);
            cacheChat.setAdminRights(newCreatorId, true);
        });

        dbService.updateChatCreatorAsync(chatId, newCreatorId);
//        dbService.makeAdminAsync(chatId, newCreatorId);
    } // КОЛХОЗ, ПОТОМ ИСПРАВЛЮ


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public void saveVerificationToken(VerificationToken verifToken) {
        cacheService.saveVerificationToken(verifToken);
        dbService.saveVerificationTokenAsync(verifToken);
    }
    public void deleteVerificationToken(String token) {
        cacheService.deleteVerificationToken(token);
        dbService.deleteVerificationTokenAsync(token);
    }
    public int cleanupExpiredTokensAndWait() {
        int numDeleted = cacheService.cleanupExpiredVerificationTokens();
        dbService.cleanupExpiredVerificationTokens();
        return numDeleted;
    }


    // Вспомогательные методы
    public Optional<VerificationToken> getVerificationToken(String token) {
        return cacheService.getVerificationToken(token);
    }


    // ========== SUB METHODS ==========


    public CacheService.CacheStats getCacheStats() {
        return cacheService.getStats();
    }
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