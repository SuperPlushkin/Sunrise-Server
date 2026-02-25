package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.*;
import com.Sunrise.Entities.DB.*;
import com.Sunrise.Repositories.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    @Async("dbExecutor")
    public void saveUserAsync(User user) {
        userRepository.save(user);
    }

    @Async("dbExecutor")
    public void deleteUserAsync(Long userId) {
        userRepository.deleteById(userId);
    }
    @Async("dbExecutor")
    public void restoreUserAsync(Long userId) {
        userRepository.restoreUser(userId);
    }

    @Async("dbExecutor")
    public void updateLastLoginAsync(String username, LocalDateTime lastLogin) {
        userRepository.updateLastLogin(username, lastLogin);
    }

    @Async("dbExecutor")
    public void enableUserAsync(Long userId) {
        userRepository.enableUser(userId);
    }


    // Вспомогательные методы
    public Optional<User> getUser(Long userId) {
        return userRepository.findById(userId);
    }
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }
    public List<User> getUsersByIds(List<Long> missingIds) {
        return userRepository.findAllById(missingIds);
    }
    public UsersPageResult getFilteredUsersPage(String filter, int offset, int limit){
        return userRepository.getUsersPage(filter, offset, limit);
    }
    public List<Chat> getUserChats(Long userId) {
        return chatRepository.findUserChats(userId);
    }


    // ========== LOGIN HISTORY METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveLoginHistoryAsync(LoginHistory loginHistory) {
        loginHistoryRepository.save(loginHistory);
    }


    // ========== CHAT METHODS ==========


    // Основные методы
    @Async("dbExecutor")
    public void saveGroupChatAsync(Chat chat, Long[] memberIds) {
        chatRepository.createGroupChat(chat.getId(), chat.getName(), chat.getCreatedBy(), memberIds);
    }

    @Async("dbExecutor")
    public void savePersonalChatAsync(Long chatId, Long creator, Long member) {
        chatRepository.createPersonalChat(chatId, creator, member);
    }

    @Async("dbExecutor")
    public void updateChatCreatorAsync(Long chatId, Long newCreatorId) {
        chatRepository.updateChatCreator(chatId, newCreatorId);
    }

    @Async("dbExecutor")
    public void restoreChatAsync(Long chatId) {
        chatRepository.restoreChat(chatId);
    }

    @Async("dbExecutor")
    public void deleteChatAsync(Long chatId) {
        chatRepository.deleteChat(chatId);
    }


    // Вспомогательные методы
    public List<Long> getAllActiveChatIds() {
        return chatRepository.findAllActiveChatIds();
    }
    public Optional<Chat> getChat(Long chatId) {
        return chatRepository.findById(chatId);
    }
    public Optional<Chat> findPersonalChat(Long userId1, Long userId2) {
        return chatRepository.findPersonalChat(userId1, userId2);
    }

    public List<Chat> getChatsByIds(List<Long> chatIds) {
        return chatRepository.findAllById(chatIds);
    }
    public ChatsPageResult getUserChatPage(Long userId, int offset, int limit) {
        return chatMemberRepository.getUserChatsPage(userId, offset, limit);
    }
    public void syncChatCounters(Long chatId) {
        chatRepository.syncChatCounters(chatId);
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


    // ========== CHAT MEMBER METHODS ==========


    @Async("dbExecutor")
    public void updateUserAdminRightsAsync(Long chatId, Long userId, Boolean isAdmin) {
        chatMemberRepository.addChatMember(chatId, userId, isAdmin);
    }

    @Async("dbExecutor")
    public void upsertChatMemberAsync(ChatMember chatMember) {
        chatMemberRepository.addChatMember(chatMember.getUserId(), chatMember.getChatId(), chatMember.isAdmin());
    }

    @Async("dbExecutor")
    public void removeUserFromChatAsync(Long userId, Long chatId) {
        chatMemberRepository.removeChatMember(chatId, userId);
    }

    public Optional<ChatMember> getChatMember(Long chatId, Long userId) {
        return chatMemberRepository.findById(new ChatMemberId(chatId, userId));
    }
    public List<Long> getUserChatIds(Long userId) {
        return chatMemberRepository.getUserChatIds(userId);
    }
    public List<Long> getMisingUserChatIds(Long userId, Set<Long> chatIds) {
        return chatMemberRepository.findMissingUserChatIds(userId, chatIds);
    }
    public List<Long> getChatMemberIds(Long chatId) {
        return chatMemberRepository.findChatMemberIds(chatId);
    }
    public List<ChatMember> getChatMembersByIds(Long chatId, List<Long> missingIds) {
        return chatMemberRepository.findChatMembersByIds(chatId, missingIds);
    }
    public ChatMembersPageResult getChatMembersPage(Long chatId, int offset, int limit) {
        return chatMemberRepository.getChatMembersPage(chatId, offset, limit);
    }

    public Optional<ChatMember> findAnotherChatAdmin(Long chatId, Long excludeUserId1) {
        return chatMemberRepository.findAnotherAdmin(chatId, excludeUserId1);
    }
    public Optional<Long> findAnotherChatAdminId(Long chatId, Long excludeUserId) {
        return chatMemberRepository.findAnotherAdminId(chatId, excludeUserId);
    }
    public Optional<Boolean> isChatAdmin(Long chatId, Long userId) {
        return chatMemberRepository.isChatAdmin(chatId, userId);
    }
    public boolean isUserInChat(Long chatId, Long userId) {
        return chatMemberRepository.isUserInChat(chatId, userId);
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


    // =========== MESSAGE METHODS ==========


    @Async("dbExecutor")
    public void saveMessageAsync(Message message) {
        messageRepository.save(message);
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
}