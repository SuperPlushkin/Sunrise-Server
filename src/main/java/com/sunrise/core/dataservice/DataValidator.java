package com.sunrise.core.dataservice;

import com.sunrise.entity.dto.ChatDTO;
import com.sunrise.helpclass.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class DataValidator {

    @Autowired
    private final DataOrchestrator dataOrchestrator;


    // ========== BASE METHODS ==========

    public void validateActiveUser(long userId) {
        if (!dataOrchestrator.isActiveUser(userId))
            throw new ValidationException("User not active: " + userId);
    }
    private void validateActiveChat(long chatId) {
        if (!dataOrchestrator.isActiveChat(chatId))
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
    }
    private void validateActiveChatMember(long chatId, long userId) {
        if (!dataOrchestrator.hasActiveChatMember(chatId, userId))
            throw new ValidationException("User " + userId + " is not a member of chat " + chatId);
    }

    public void validateActiveGroupChat(long chatId) {
        Optional<Boolean> isGroup = dataOrchestrator.isGroupChat(chatId);
        if (isGroup.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        if (!isGroup.get()) {
            throw new ValidationException("Chat is a personal chat: " + chatId);
        }
    }
    private ChatDTO validateActiveChatAndGet(long chatId) {
        Optional<ChatDTO> chatOpt = dataOrchestrator.getActiveChat(chatId);
        if (chatOpt.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        return chatOpt.get();
    }
    private void validateActiveChatMemberIsAdmin(long chatId, long userId){
        Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("Inviter is not a member of this chat");
        }
        if (!isAdmin.get()) {
            throw new ValidationException("Only admin can add members to group");
        }
    }


    // ========== OTHER METHODS ==========

    public void validateActiveUsers(long userId, Set<Long> userIds) {
        validateActiveUser(userId);
        userIds.forEach(this::validateActiveUser);
    }
    public void validateActiveUsers(long userId, long otherUserId) {
        validateActiveUser(userId);
        validateActiveUser(otherUserId);
    }

    public void validateActiveChatMemberInActiveChat(long chatId, long userId) {
        validateActiveUser(userId);
        validateActiveChat(chatId);
        validateActiveChatMember(chatId, userId);
    }
    public void validateActiveUsersInActiveChatAndOneIsAdmin(long chatId, long adminId, long otherUserId) {
        validateActiveUsers(adminId, otherUserId);
        validateActiveChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, adminId);
    }
    public ChatDTO validateActiveUserInActiveChatAndGetChat(long chatId, long userId) {
        validateActiveUser(userId);
        ChatDTO chat = validateActiveChatAndGet(chatId);
        validateActiveChatMember(chatId, userId);
        return chat;
    }

    public void validateCanUpdateChatInfo(long chatId, long userId) {
        validateActiveUser(userId);
        ChatDTO chat = validateActiveChatAndGet(chatId);
        if (chat.isPersonal()) {
            throw new ValidationException("Chat info is not changeable for private chat");
        }
        validateActiveChatMemberIsAdmin(chatId, userId);
    }

    public void validateAddChatMembers(long chatId, long inviterId, Set<Long> newUserIds) {
        validateActiveUsers(inviterId, newUserIds);
        validateActiveGroupChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, inviterId);
        newUserIds.forEach(id -> validateActiveChatMember(chatId, id));
    }
    public void validateAddChatMember(long chatId, long inviterId, long newUserId) {
        validateActiveUsers(inviterId, newUserId);
        validateActiveGroupChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, inviterId);
        validateActiveChatMember(chatId, newUserId);
    }

    public void validateCanSendPrivateMessage(long chatId, long senderId, long userToSend) {
        validateActiveUsers(senderId, userToSend);
        validateActiveGroupChat(chatId);
        validateActiveChatMember(chatId, senderId);
        validateActiveChatMember(chatId, userToSend);
    }
    public void validateCanDeleteChat(long chatId, long userId) {
        validateActiveChatMemberInActiveChat(chatId, userId);
        validateActiveChatMemberIsAdmin(chatId, userId);
    }

    public void validateActiveMessageInChat(long chatId, long messageId) {
        if (!dataOrchestrator.isActiveMessageInChat(chatId, messageId)){
            throw new ValidationException("Message not exists or is deleted: " + messageId);
        }
    }
    public void validateActiveMessageInChatAndIsSender(long chatId, long userId, long messageId) {
        if (!dataOrchestrator.isActiveMessageInChatAndIsSender(chatId, userId, messageId)){
            throw new ValidationException("Message not exists or is deleted: " + messageId);
        }
    }

    public void validateCanUpdateMessage(long chatId, long userId, long messageId) {
        validateActiveUser(userId);
        Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("Member not exists or is deleted: " + messageId);
        }

        if (isAdmin.get()) {
            validateActiveMessageInChat(chatId, messageId);
        } else {
            validateActiveMessageInChatAndIsSender(chatId, userId, messageId);
        }
    }
    public void validateCanDeleteMessage(long chatId, long userId, long messageId) {
        validateActiveUser(userId);
        Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("Member not exists or is deleted: " + messageId);
        }

        if (isAdmin.get()) {
            validateActiveMessageInChat(chatId, messageId);
        } else {
            validateActiveMessageInChatAndIsSender(chatId, userId, messageId);
        }
    }
}