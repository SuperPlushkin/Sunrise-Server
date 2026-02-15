package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.DTO.DBResults.ChatMemberDBResult;
import com.Sunrise.DTO.DBResults.PersonalChatDBResult;
import com.Sunrise.DTO.DBResults.MessageDBResult;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Repositories.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DBService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final VerificationTokenRepository tokenRepository;
    private final MessageRepository messageRepository;

    public DBService(UserRepository userRepository, ChatRepository chatRepository, LoginHistoryRepository loginHistoryRepository,
                     VerificationTokenRepository tokenRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.tokenRepository = tokenRepository;
        this.messageRepository = messageRepository;
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
        updateLastLogin(username, lastLogin);
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
    public List<User> getFilteredUsers(String filter, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        return userRepository.findFilteredUsers(filter, pageable);
    }
    public Boolean existsUser(Long userId) {
        return userRepository.existsById(userId);
    }
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public List<Chat> getUserChats(Long userId) {
        return chatRepository.findUserChats(userId);
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


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveChat(Chat chat) {
        chatRepository.save(chat);
    }
    @Async("dbExecutor")
    public void saveChatAsync(Chat chat) {
        saveChat(chat);
    }

    public void restoreChat(Long chatId) {
        chatRepository.restoreChat(chatId);
    }
    @Async("dbExecutor")
    public void restoreChatAsync(Long chatId) {
        restoreChat(chatId);
    }

    public void deleteChat(Long chatId) {
        chatRepository.softDeleteChat(chatId);
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
    public List<PersonalChatDBResult> getAllPersonalChats() {
        return chatRepository.getAllPersonalChats();
    }
    public List<ChatMemberDBResult> getAllChatMembers() {
        return chatRepository.getAllChatMembers();
    }
    public List<Chat> getAllChats() {
        return chatRepository.findAll();
    }
    public Optional<Chat> getChat(Long chatId) {
        return chatRepository.findById(chatId);
    }

    public Optional<Long> findPersonalChat(Long userId1, Long userId2) {
        return chatRepository.findPersonalChat(userId1, userId2);
    }
    public Optional<Long> findDeletedPersonalChat(Long userId1, Long userId2) {
        return chatRepository.findDeletedPersonalChat(userId1, userId2);
    }
    public List<ChatMemberDBResult> getChatMembers(Long chatId) {
        return chatRepository.getChatMembers(chatId);
    }
    public boolean isUserInChat(Long chatId, Long userId) {
        return chatRepository.isUserInChat(chatId, userId);
    }
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        return chatRepository.isChatAdmin(chatId, userId);
    }
    public Optional<Long> findAnotherAdmin(Long chatId, Long excludeUserId) {
        return chatRepository.findAnotherAdmin(chatId, excludeUserId);
    }


    // Методы для работы с историей чата
    public Integer clearChatHistoryForAll(Long chatId, Long userId) {
        return chatRepository.clearChatHistoryForAll(chatId, userId);
    }
    public Integer clearChatHistoryForSelf(Long chatId, Long userId) {
        return chatRepository.clearChatHistoryForSelf(chatId, userId);
    }
    public ChatStatsDBResult getChatClearStats(Long chatId, Long userId) {
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
        saveVerificationToken(token);
    }

    public void deleteVerificationToken(String token) {
        tokenRepository.deleteByToken(token);
    }
    @Async("dbExecutor")
    public void deleteVerificationTokenAsync(String token) {
        deleteVerificationToken(token);
    }

    public void cleanupExpiredVerificationTokens() {
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }


    // =========== MESSAGE METHODS ==========

    public void saveMessage(Message message) {
        messageRepository.save(message);
    }
    @Async("dbExecutor")
    public void saveMessageAsync(Message message) {
        saveMessage(message);
    }


    // Вспомогательные методы
    public List<MessageDBResult> getChatMessagesFirst(Long chatId, Long userId, Integer limit) {
        return messageRepository.getChatMessagesFirst(chatId, userId, limit);
    }
    public List<MessageDBResult> getChatMessagesBefore(Long chatId, Long userId, Long messageId, Integer limit) {
        return messageRepository.getChatMessagesBefore(chatId, userId, messageId, limit);
    }
    public List<MessageDBResult> getChatMessagesAfter(Long chatId, Long userId, Long messageId, Integer limit) {
        return messageRepository.getChatMessagesAfter(chatId, userId, messageId, limit);
    }

    public void markMessageAsRead(Long messageId, Long userId) {
        messageRepository.markMessageAsRead(messageId, userId);
    }
    public Integer getVisibleMessagesCount(Long chatId, Long userId) {
        return messageRepository.getVisibleMessagesCount(chatId, userId);
    }

    public List<Long> getUserChatIds(Long userId) {
        return chatRepository.getUserChatIds(userId);
    }
    public List<Chat> getChatsByIds(Set<Long> chatIds) {
        if (chatIds.isEmpty())
            return Collections.emptyList();
        return chatRepository.findAllById(chatIds);
    }
}