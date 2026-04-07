package com.sunrise.core.dataservice;

import com.sunrise.core.dataservice.type.*;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.entity.db.*;
import com.sunrise.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public int deleteUser(long userId) {
        return userRepository.deleteUser(userId);
    }
    public int restoreUser(long userId) {
        return userRepository.restoreUser(userId);
    }
    @Async("dbExecutor")
    public void updateLastLoginAsync(String username, LocalDateTime lastLogin) {
        userRepository.updateLastLogin(username, lastLogin);
    }
    public int updateUserProfile(long userId, String username, String name) {
        return userRepository.updateProfile(userId, username, name);
    }
    public int enableUser(long userId) {
        return userRepository.enableUser(userId);
    }


    // Вспомогательные методы
    public Optional<User> getUser(long userId) {
        return userRepository.findById(userId);
    }
    public Optional<User> getUserByUsername(String username) {
        return userRepository.getByUsername(username);
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.getByEmail(email);
    }

    public List<User> getActiveUserByIds(List<Long> missingIds) {
        return userRepository.getActiveUserByIds(missingIds);
    }
    public List<UserResult> getActiveUsersPage(String filter, Long cursor, int limit) {
        return userRepository.getActiveUsersPage(filter, cursor, Pageable.ofSize(limit));
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    public void saveGroupChat(Chat chat, Long[] memberIds, Boolean[] isAdminFlags) {
        chatRepository.saveGroupChatAndMembers(
            chat.getId(), chat.getName(),
            memberIds, isAdminFlags,
            chat.getCreatedBy(), chat.getCreatedAt()
        );
    }
    public void savePersonalChat(Chat chat, long opponentId) {
        chatRepository.savePersonalChatAndMembers(chat.getId(), chat.getCreatedBy(), opponentId, chat.getCreatedAt());
    }

    public int restoreChat(long chatId) {
        return chatRepository.restoreChat(chatId);
    }
    public int deleteChat(long chatId) {
        return chatRepository.deleteChat(chatId);
    }


    // Вспомогательные методы
    public Optional<Chat> getFullChat(long chatId) {
        return chatRepository.findById(chatId);
    }
    public Optional<Chat> getFullPersonalChat(long userId1, long userId2) {
        return chatRepository.getPersonalChat(userId1, userId2);
    }
    public List<UserFullChatResult> getFullUserChatsPage(long userId, Long cursor, int limit) {
        return chatRepository.getUserChatsPage(userId, cursor, limit);
    }


    // ========== CHAT MEMBER METHODS ==========


    // Основные методы
    public int updateUserAdminRights(long chatId, long userId, boolean isAdmin) {
        return chatMemberRepository.updateAdminRights(chatId, userId, isAdmin);
    }
    public void upsertChatMember(ChatMember chatMember) {
        chatMemberRepository.saveOrRestoreChatMember(chatMember.getChatId(), chatMember.getUserId(), chatMember.isAdmin());
    }
    public void upsertChatMembers(long chatId, Long[] memberIds, Boolean[] isAdminFlags) {
        chatMemberRepository.saveOrRestoreChatMembers(chatId, memberIds, isAdminFlags);
    }
    public boolean removeUserFromChat(long userId, long chatId) {
        return chatMemberRepository.removeChatMember(chatId, userId);
    }


    // Вспомогательные методы
    public Optional<ChatMember> getChatMember(long chatId, long userId) {
        return chatMemberRepository.findById(new ChatMemberId(chatId, userId));
    }
    public Optional<ChatMember> getActiveChatMember(long chatId, long userId) {
        return chatMemberRepository.getActiveChatMember(chatId, userId);
    }
    public List<ChatMember> getActiveChatMembersByIds(long chatId, List<Long> missingIds) {
        return chatMemberRepository.getActiveChatMembersByIds(chatId, missingIds);
    }

    public List<Long> getChatMemberIdsPage(long chatId, Long cursor, int limit) {
        return chatMemberRepository.getChatMemberIdsPage(chatId, cursor, PageRequest.of(0, limit));
    }


    // ========== VERIFICATION TOKEN METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveVerificationTokenAsync(VerificationToken token) {
        tokenRepository.save(token);
    }
    @Async("dbExecutor")
    public void deleteVerificationTokenAsync(String token) {
        tokenRepository.deleteByToken(token);
    }
    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.getByToken(token);
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
    public void saveMessage(Message message) {
        messageRepository.save(message);
    }
    public void markMessagesUpToRead(long chatId, long userId, long messageId, LocalDateTime readAt) {
        messageRepository.markMessagesUpToRead(chatId, userId, messageId, readAt, "7 days");
    }
    public int restoreMessage(long messageId) {
        return messageRepository.restoreMessage(messageId);
    }
    public int deleteMessage(long messageId) {
        return messageRepository.deleteMessage(messageId);
    }

    // Вспомогательные методы
    public List<UserMessageDBResult> getMessagePage(long chatId, long userId, Long cursor, int limit, Direction direction) {
        Pageable pageable = PageRequest.of(0, limit);
        if (cursor == null) {
            return messageRepository.getFirstMessagePage(chatId, userId, pageable);
        }
        if (direction == Direction.FORWARD) {
            return messageRepository.getMessagePageAfter(chatId, userId, cursor, pageable);
        }

        return messageRepository.getMessagePageBefore(chatId, userId, cursor, pageable);
    }

    public ChatStatsDBResult getChatMessagesDeletedStats(long chatId, long userId) {
        return chatRepository.getChatClearStats(chatId, userId);
    }

    public Optional<Message> getMessage(long messageId) {
        return messageRepository.findById(messageId);
    }
    public Optional<UserMessageDBResult> getMessageWithReadStatus(long userId, long messageId) {
        return messageRepository.getMessageById(userId, messageId);
    }
    public List<MessageReadStatusResult> getMessageReaders(long messageId){
        return messageRepository.getMessageReaders(messageId);
    }
}