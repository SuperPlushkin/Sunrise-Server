package com.Sunrise.Services.DataServices.Interfaces;

import com.Sunrise.DTO.DBResults.ChatStatsResult;
import com.Sunrise.DTO.DBResults.GetChatMemberResult;
import com.Sunrise.DTO.DBResults.GetPersonalChatResult;
import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.Entities.Chat;
import com.Sunrise.Entities.LoginHistory;
import com.Sunrise.Entities.User;
import com.Sunrise.Entities.VerificationToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Асинхронный сервис для работы с постоянным хранилищем (БД)
 * Все методы работают с основными данными приложения
 */
public interface IAsyncStorageService extends IStorageService {


    // ========== USER METHODS ==========

    List<User> getAllUsers();
    Optional<User> getUser(Long userId);
    Optional<User> getUserByUsername(String username);
    Optional<List<Chat>> getUserChats(Long userId);

    Boolean existsUserByUsername(String username);
    Boolean existsUserByEmail(String email);

    void addUserToChat(Long userId, Long chatId, Boolean isAdmin);
    void addUserToChatAsync(Long userId, Long chatId, Boolean isAdmin);

    void saveUser(User user);
    void saveUserAsync(User user);
    void deleteUserAsync(Long userId);

    void removeUserFromChatAsync(Long userId, Long chatId);

    void updateLastLogin(String username, LocalDateTime lastLogin);
    void updateLastLoginAsync(String username, LocalDateTime lastLogin);

    void enableUser(Long userId);
    void enableUserAsync(Long userId);


    // ========== CHAT METHODS ==========

    List<Chat> getAllChats();
    Optional<Chat> getChat(Long chatId);

    void saveChatAsync(Chat chat);
    void deleteChatAsync(Long chatId);

    List<GetPersonalChatResult> getAllPersonalChats();
    Long findExistingPersonalChat(Long userId1, Long userId2);

    Boolean isChatAdmin(Long chatId, Long userId);
    Long getChatCreator(Long chatId);
    List<GetChatMemberResult> getAllChatMembers();
    Integer getChatMemberCount(Long chatId);
    Optional<Long> findAnotherAdmin(Long chatId, Long excludeUserId);
    void updateChatCreator(Long chatId, Long newCreatorId);
    void updateChatCreatorAsync(Long chatId, Long newCreatorId);

    List<MessageResult> getChatMessages(Long chatId, Long userId, Integer limit, Integer offset);
    void markMessageAsRead(Long messageId, Long userId);
    Integer getVisibleMessagesCount(Long chatId, Long userId);

    Integer clearChatHistoryForAll(Long chatId, Long userId);
    Integer clearChatHistoryForSelf(Long chatId, Long userId);
    Integer restoreChatHistoryForSelf(Long chatId, Long userId);

    ChatStatsResult getChatClearStats(Long chatId, Long userId);


    // ========== LOGIN HISTORY METHODS ==========

    void saveLoginHistory(LoginHistory loginHistory);
    void saveLoginHistoryAsync(LoginHistory loginHistory);
    void addLoginHistory(Long userId, String ipAddress, String deviceInfo);
    void addLoginHistoryAsync(Long userId, String ipAddress, String deviceInfo);


    // ========== VERIFICATION TOKEN METHODS ==========

    void saveVerificationTokenAsync(VerificationToken token);
    List<VerificationToken> getAllVerificationTokens();
    void deleteVerificationTokenAsync(String token);
    void cleanupExpiredVerificationTokensAsync();
}