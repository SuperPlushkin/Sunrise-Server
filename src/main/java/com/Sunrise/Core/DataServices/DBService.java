package com.Sunrise.Core.DataServices;

import com.Sunrise.DTOs.DBResults.*;
import com.Sunrise.DTOs.Paginations.*;
import com.Sunrise.Entities.DBs.*;
import com.Sunrise.Repositories.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class DBService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final VerificationTokenRepository tokenRepository;
    private final MessageRepository messageRepository;

    public DBService(UserRepository userRepository, ChatRepository chatRepository, LoginHistoryRepository loginHistoryRepository,
                     VerificationTokenRepository tokenRepository, MessageRepository messageRepository, ChatMemberRepository chatMemberRepository) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
        this.loginHistoryRepository = loginHistoryRepository;
        this.tokenRepository = tokenRepository;
        this.messageRepository = messageRepository;
        this.chatMemberRepository = chatMemberRepository;
    }


    // ========== USER METHODS ==========


    // Основные методы
    public void saveUser(User user) {
        userRepository.save(user);
    }
    public void deleteUser(long userId) {
        userRepository.deleteById(userId);
    }
    public void restoreUser(long userId) {
        userRepository.restoreUser(userId);
    }
    @Async("dbExecutor")
    public void updateLastLoginAsync(String username, LocalDateTime lastLogin) {
        userRepository.updateLastLogin(username, lastLogin);
    }
    public void updateUserProfile(long userId, String username, String name) {
        userRepository.updateProfile(userId, username, name);
    }
    public void enableUser(long userId) {
        userRepository.enableUser(userId);
    }


    // Вспомогательные методы
    public Optional<User> getUser(long userId) {
        return userRepository.findById(userId);
    }
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getActiveUserByIds(List<Long> missingIds) {
        return userRepository.findActiveUserByIds(missingIds);
    }
    public List<UserResult> getFullFilteredUsersPage(String filter, Long cursor, int limit) {
        return userRepository.getFullFilteredUsersPage(filter, cursor, limit);
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveGroupChat(Chat chat, Long[] memberIds) {
        chatRepository.createGroupChat(chat.getId(), chat.getName(), chat.getCreatedBy(), memberIds, chat.getCreatedAt());
    }
    public void savePersonalChat(Chat chat, long opponentId) {
        chatRepository.createPersonalChat(chat.getId(), chat.getCreatedBy(), opponentId, chat.getCreatedAt());
    }

    public void restoreChat(long chatId) {
        chatRepository.restoreChat(chatId);
    }
    public void deleteChat(long chatId) {
        chatRepository.deleteChat(chatId);
    }


    // Вспомогательные методы
    public Optional<FullChatResult> getFullChat(long chatId) {
        return chatRepository.findFullChat(chatId);
    }
    public Optional<FullChatResult> getFullPersonalChat(long userId1, long userId2) {
        return chatRepository.findPersonalChat(userId1, userId2);
    }

    public List<UserFullChatResult> getUserFullChatsByChatIds(Set<Long> chatIds, Long userId) {
        return chatRepository.findFullChats(chatIds, userId);
    }
    public List<UserFullChatResult> getFullUserChatsPage(long userId, Long cursor, int limit) {
        return chatRepository.getUserChatsPage(userId, cursor, limit);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public void updateUserAdminRights(long chatId, long userId, boolean isAdmin) {
        chatMemberRepository.addChatMember(chatId, userId, isAdmin);
    }
    public void upsertChatMember(ChatMember chatMember) {
        chatMemberRepository.addChatMember(chatMember.getUserId(), chatMember.getChatId(), chatMember.isAdmin());
    }
    public void removeUserFromChat(long userId, long chatId) {
        chatMemberRepository.removeChatMember(chatId, userId);
    }


    // Вспомогательные методы
    public Optional<ChatMember> getChatMember(long chatId, long userId) {
        return chatMemberRepository.findById(new ChatMemberId(chatId, userId));
    }
    public Optional<ChatMember> getActiveChatMember(long chatId, long userId) {
        return chatMemberRepository.findById(new ChatMemberId(chatId, userId));
    }

    public List<Long> getChatMemberIds(long chatId) {
        return chatMemberRepository.findChatMemberIds(chatId);
    }
    public List<ChatMember> getActiveChatMembersByIds(long chatId, List<Long> missingIds) {
        return chatMemberRepository.findActiveChatMembersByIds(chatId, missingIds);
    }

    public List<ChatOpponentResult> findOpponentsForChats(Set<Long> chatIds, long excludingId) {
        return chatMemberRepository.findOpponentsForChats(chatIds, excludingId);
    }

    public List<ChatMemberResult> getFullChatMembersPage(long chatId, Long cursor, int limit) {
        return chatMemberRepository.findFullChatMembersPage(chatId, cursor, limit);
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Async("dbExecutor")
    public void saveVerificationTokenAsync(VerificationToken token) {
        tokenRepository.save(token);
    }

    @Async("dbExecutor")
    public void deleteVerificationTokenAsync(String token) {
        tokenRepository.deleteByToken(token);
    }

    public int cleanupExpiredVerificationTokens() {
        return tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveLoginHistoryAsync(LoginHistory loginHistory) {
        loginHistoryRepository.save(loginHistory);
    }


    // =========== MESSAGE METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveMessageAsync(Message message) {
        messageRepository.save(message);
    }
    public void markMessageAsRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        messageRepository.markMessageAsRead(chatId, userId, messageId, readAt);
    }


    // Вспомогательные методы
    public List<UserMessageDBResult> getChatMessagesFirst(long chatId, long userId, int limit) {
        return messageRepository.getChatMessagesFirst(chatId, userId, limit);
    }

    public List<UserMessageDBResult> getChatMessagesBefore(long chatId, long userId, long messageId, int limit) {
        return messageRepository.getChatMessagesBefore(chatId, userId, messageId, limit);
    }
    public List<UserMessageDBResult> getMessagesWithGapCheckBefore(long chatId, long userId, long beforeMessageId, Long lastKnownId, int limit, int maxGap) {
        return messageRepository.getMessagesWithGapCheckBefore(chatId, userId, beforeMessageId, lastKnownId, limit, maxGap);
    }

    public List<UserMessageDBResult> getChatMessagesAfter(long chatId, long userId, long messageId, int limit) {
        return messageRepository.getChatMessagesAfter(chatId, userId, messageId, limit);
    }
    public List<UserMessageDBResult> getMessagesWithGapCheckAfter(long chatId, long userId, long afterMessageId, Long lastKnownId, int limit, int maxGap) {
        return messageRepository.getMessagesWithGapCheckAfter(chatId, userId, afterMessageId, lastKnownId, limit, maxGap);
    }

    public List<LastUserReadStatusResult> getUserReadStatusByChatIds(long userId, Set<Long> chatIds) {
        return messageRepository.getUserReadStatusByChatIds(chatIds, userId);
    }
    public Optional<Long> getUserReadStatusByChatId(long chatId, long userId) {
        return messageRepository.getUserReadStatusByChatId(chatId, userId);
    }

    public ChatStatsDBResult getChatMessagesDeletedStats(long chatId, long userId) {
        return chatRepository.getChatClearStats(chatId, userId);
    }
}