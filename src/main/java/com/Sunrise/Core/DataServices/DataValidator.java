package com.Sunrise.Core.DataServices;

import com.Sunrise.Entities.DTOs.LightChatDTO;
import com.Sunrise.Entities.DTOs.FullUserDTO;
import com.Sunrise.Subclasses.ValidationException;
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


    // ========== PUBLIC METHODS ==========

    public void validateActiveUser(long userId) {
        Optional<FullUserDTO> userOpt = dataOrchestrator.getUser(userId);
        if (userOpt.isEmpty())
            throw new ValidationException("User not found: " + userId);

        FullUserDTO user = userOpt.get();
        if (user.isDeleted()) {
            throw new ValidationException("User is deleted: " + userId);
        }
        if (!user.isEnabled()) {
            throw new ValidationException("User is not verified: " + userId);
        }
    }
    public void validateActiveUsers(long userId, Set<Long> userIds) {
        validateActiveUser(userId);
        userIds.forEach(this::validateActiveUser);
    }
    public void validateActiveChatMemberInActiveChat(long chatId, long userId) {
        validateActiveUser(userId);
        validateActiveChat(chatId);
        validateActiveChatMember(chatId, userId);
    }
    public void validateActiveUsersInActiveChatAndOneIsAdmin(long chatId, long adminId, long otherUserId) {
        validateActiveUser(adminId);
        validateActiveUser(otherUserId);
        validateActiveChat(chatId);
        validateActiveChatMemberIsAdmin(chatId, adminId);
    }
    public LightChatDTO validateActiveUserInActiveChatAndGetChat(long chatId, long userId) {
        validateActiveUser(userId);
        LightChatDTO chat = validateActiveChatAndGet(chatId);
        validateActiveChatMember(chatId, userId);
        return chat;
    }
    public void validateActiveUsersInActiveChatAndChatIsPersonal(long chatId, Set<Long> userIds) {
        if (!validateActiveChatAndGetIsGroup(chatId))
           throw new ValidationException("Chat is a personal chat");

        for (long userId : userIds) {
            validateActiveUser(userId);
            validateActiveChatMember(chatId, userId);
        }
    }

    public void validateAddGroupMembers(long chatId, long inviterId, Set<Long> newUserIds) {
        // проверяем пользователей
        validateActiveUsers(inviterId, newUserIds);

        // проверяем, что это групповой чат
        if (!validateActiveChatAndGetIsGroup(chatId))
            throw new ValidationException("Cannot add members to personal chat");

        validateActiveChatMemberIsAdmin(chatId, inviterId);
        newUserIds.forEach(id -> validateActiveChatMember(chatId, id));
    }

    public void validateAddGroupMember(long chatId, long inviterId, long newUserId) {
        // проверяем пользователей
        validateActiveUser(inviterId);
        validateActiveUser(newUserId);

        // проверяем, что это групповой чат
        if (!validateActiveChatAndGetIsGroup(chatId))
            throw new ValidationException("Cannot add members to personal chat");

        validateActiveChatMemberIsAdmin(chatId, inviterId);
        validateActiveChatMember(chatId, newUserId);
    }


    // ========== PRIVATE METHODS ==========

    private void validateActiveChat(long chatId) {
        if (!dataOrchestrator.isActiveChat(chatId))
            throw new ValidationException("Chat does not existttttt or is deleted: " + chatId);
    }
    private Boolean validateActiveChatAndGetIsGroup(long chatId) {
        Optional<Boolean> isGroup = dataOrchestrator.isGroupChat(chatId);
        if (isGroup.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        return isGroup.get();
    }
    private LightChatDTO validateActiveChatAndGet(long chatId) {
        Optional<LightChatDTO> chatOpt = dataOrchestrator.getActiveChat(chatId);
        if (chatOpt.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        return chatOpt.get();
    }
    private void validateActiveChatMember(long chatId, long userId) {
        if (!dataOrchestrator.hasActiveChatMember(chatId, userId))
            throw new ValidationException("User " + userId + " is not a member of chat " + chatId);
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


    public void validateCanDeleteChat(long chatId, long userId) {
        validateActiveChatMemberInActiveChat(chatId, userId);
        validateActiveChatMemberIsAdmin(chatId, userId);
    }
}