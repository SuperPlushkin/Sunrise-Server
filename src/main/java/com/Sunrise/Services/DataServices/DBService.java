package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsResult;
import com.Sunrise.DTO.DBResults.GetChatMemberResult;
import com.Sunrise.DTO.DBResults.GetPersonalChatResult;
import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.Entities.LoginHistory;
import com.Sunrise.Entities.User;
import com.Sunrise.Entities.Chat;
import com.Sunrise.Entities.VerificationToken;
import com.Sunrise.Repositories.LoginHistoryRepository;
import com.Sunrise.Repositories.UserRepository;
import com.Sunrise.Repositories.ChatRepository;
import com.Sunrise.Repositories.VerificationTokenRepository;
import com.Sunrise.Services.DataServices.Interfaces.IAsyncStorageService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DBService implements IAsyncStorageService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final VerificationTokenRepository tokenRepository;

    public DBService(UserRepository userRepository, ChatRepository chatRepository, LoginHistoryRepository loginHistoryRepository, VerificationTokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.tokenRepository = tokenRepository;
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(User user) {
        userRepository.save(user);
    }
    @Async("dbExecutor")
    public void saveUserAsync(User user) {
        saveUser(user);
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    @Async("dbExecutor")
    public void deleteUserAsync(Long userId) {
        deleteUser(userId);
    }

    public void updateLastLogin(String username, LocalDateTime lastLogin) {
        userRepository.updateLastLogin(username, lastLogin);
    }
    @Async("dbExecutor")
    public void updateLastLoginAsync(String username, LocalDateTime lastLogin) {
        userRepository.updateLastLogin(username, lastLogin);
    }
    public void enableUser(Long userId) {
        userRepository.enableUser(userId);
    }
    @Async("dbExecutor")
    public void enableUserAsync(Long userId) {
        enableUser(userId);
    }

    // Вспомогательные методы
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public boolean existsUser(Long userId) {
        return userRepository.existsById(userId);
    }
    public Boolean existsUserByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    public Boolean existsUserByEmail(String email) {
        return userRepository.existsByEmail(email);
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    public void saveLoginHistory(LoginHistory loginHistory) {
        loginHistoryRepository.save(loginHistory);
    }
    @Async("dbExecutor")
    public void saveLoginHistoryAsync(LoginHistory loginHistory) {
        saveLoginHistory(loginHistory);
    }

    public void addLoginHistory(Long userId, String ipAddress, String deviceInfo) {
        loginHistoryRepository.addLoginHistory(userId, ipAddress, deviceInfo);
    }
    @Async("dbExecutor")
    public void addLoginHistoryAsync(Long userId, String ipAddress, String deviceInfo) {
        addLoginHistory(userId, ipAddress, deviceInfo);
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChat(Chat chat) {
        chatRepository.save(chat);
    }
    @Async("dbExecutor")
    public void saveChatAsync(Chat chat) {
        saveChat(chat);
    }

    public void deleteChat(Long chatId) {
        chatRepository.deleteChat(chatId);
    }
    @Async("dbExecutor")
    public void deleteChatAsync(Long chatId) {
        deleteChat(chatId);
    }

    public void addUserToChat(Long userId, Long chatId, Boolean isAdmin) {
        chatRepository.upsertChatMember(chatId, userId, isAdmin);
    }
    @Async("dbExecutor")
    public void addUserToChatAsync(Long userId, Long chatId, Boolean isAdmin) {
        addUserToChat(userId, chatId, isAdmin);
    }

    public void removeUserFromChat(Long userId, Long chatId) {
        chatRepository.leaveChat(chatId, userId);
    }
    @Async("dbExecutor")
    public void removeUserFromChatAsync(Long userId, Long chatId) {
        removeUserFromChat(userId, chatId);
    }

    public void updateChatCreator(Long chatId, Long newCreatorId) {
        chatRepository.updateChatCreator(chatId, newCreatorId);
    }
    @Async("dbExecutor")
    public void updateChatCreatorAsync(Long chatId, Long newCreatorId) {
        updateChatCreator(chatId, newCreatorId);
    }


    // Вспомогательные методы
    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }
    public Optional<Chat> getChat(Long chatId) {
        return chatRepository.findById(chatId);
    }
    public Optional<List<Chat>> getUserChats(Long userId) {
        return Optional.ofNullable(null);
    } // TODO: Реализовать надо метод
    public boolean existsChat(Long chatId) {
        return chatRepository.existsById(chatId);
    }
    public boolean isUserInChat(Long chatId, Long userId) {
        return chatRepository.isChatMember(chatId, userId);
    }


    // Методы для работы с личными чатами
    public List<GetPersonalChatResult> getAllPersonalChats() {
        return chatRepository.getAllPersonalChats();
    }
    public Long findExistingPersonalChat(Long userId1, Long userId2) {
        return chatRepository.findExistingPersonalChat(userId1, userId2);
    }


    // Методы для работы с правами администратора
    public Boolean isChatAdmin(Long chatId, Long userId) {
        return chatRepository.isChatAdmin(chatId, userId);
    }
    public Long getChatCreator(Long chatId) {
        return chatRepository.getChatCreator(chatId);
    }
    public Optional<Long> findAnotherAdmin(Long chatId, Long excludeUserId) {
        return chatRepository.findAnotherAdmin(chatId, excludeUserId);
    }


    // Методы для работы с участниками чата
    public List<GetChatMemberResult> getAllChatMembers() {
        return chatRepository.getAllChatMembers();
    }
    public Integer getChatMemberCount(Long chatId) {
        return chatRepository.getChatMemberCount(chatId);
    }


    // Методы для работы с сообщениями
    public List<MessageResult> getChatMessages(Long chatId, Long userId, Integer limit, Integer offset) {
        return chatRepository.getChatMessages(chatId, userId, limit, offset);
    }
    public void markMessageAsRead(Long messageId, Long userId) {
        chatRepository.markMessageAsRead(messageId, userId);
    }
    public Integer getVisibleMessagesCount(Long chatId, Long userId) {
        return chatRepository.getVisibleMessagesCount(chatId, userId);
    }


    // Методы для работы с историей чата
    public Integer clearChatHistoryForAll(Long chatId, Long userId) {
        return chatRepository.clearChatHistoryForAll(chatId, userId);
    }
    public Integer clearChatHistoryForSelf(Long chatId, Long userId) {
        return chatRepository.clearChatHistoryForSelf(chatId, userId);
    }
    public Integer restoreChatHistoryForSelf(Long chatId, Long userId) {
        return chatRepository.restoreChatHistoryForSelf(chatId, userId);
    }
    public ChatStatsResult getChatClearStats(Long chatId, Long userId) {
        return chatRepository.getChatClearStats(chatId, userId);
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public List<VerificationToken> getAllVerificationTokens() {
        return tokenRepository.findAll();
    }
    public void saveVerificationToken(VerificationToken token) {
        tokenRepository.save(token);
    }

    @Async("dbExecutor")
    public void saveVerificationTokenAsync(VerificationToken token) {
        tokenRepository.save(token);
    }

    public void deleteVerificationToken(String token) {
        tokenRepository.deleteByToken(token);
    }
    @Async("dbExecutor")
    public void deleteVerificationTokenAsync(String token) {
        deleteVerificationToken(token);
    }

    public int cleanupExpiredVerificationTokens() {
        LocalDateTime now = LocalDateTime.now();
        return tokenRepository.deleteByExpiryDateBefore(now);
    }
    @Async("dbExecutor")
    public void cleanupExpiredVerificationTokensAsync() {
        cleanupExpiredVerificationTokens();
    }

    // Вспомогательные методы
    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }
}