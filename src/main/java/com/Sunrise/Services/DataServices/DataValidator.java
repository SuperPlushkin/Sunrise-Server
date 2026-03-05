package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.Entities.DTO.ChatDTO;
import com.Sunrise.Entities.DB.User;
import com.Sunrise.Entities.DTO.FullUserDTO;
import com.Sunrise.Subclasses.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class DataValidator {

    private final DataAccessService dataAccessService;


    // ========== PUBLIC METHODS ==========

    public void validateActiveUser(long userId) {
        Optional<FullUserDTO> userOpt = dataAccessService.getUser(userId);
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
    public ChatDTO validateActiveUserInActiveChatAndGetChat(long chatId, long userId) {
        validateActiveUser(userId);
        ChatDTO chat = validateActiveChatAndGet(chatId);
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
    public void validateCanClearForAll(long chatId, long userId) {
        validateActiveChatMemberInActiveChat(chatId, userId);

        // проверяем, что приглашающий - админ
        validateActiveChatMemberIsAdmin(chatId, userId);

        ChatStatsDBResult stats = dataAccessService.getChatClearStats(chatId, userId);
        if (!stats.getCanDeleteForAll())
            throw new ValidationException("User does not have permission to clear chat for all");
    }


    // ========== PRIVATE METHODS ==========

    private void validateActiveChat(long chatId) {
        if (!dataAccessService.ensureActiveChat(chatId))
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
    }
    private Boolean validateActiveChatAndGetIsGroup(long chatId) {
        Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);
        if (isGroup.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        return isGroup.get();
    }
    private ChatDTO validateActiveChatAndGet(long chatId) {
        Optional<ChatDTO> chatOpt = dataAccessService.getActiveChat(chatId);
        if (chatOpt.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        return chatOpt.get();
    }
    private void validateActiveChatMember(long chatId, long userId) {
        if (!dataAccessService.hasActiveChatMember(chatId, userId))
            throw new ValidationException("User " + userId + " is not a member of chat " + chatId);
    }
    private void validateActiveChatMemberIsAdmin(long chatId, long userId){
        Optional<Boolean> isAdmin = dataAccessService.isActiveAdminInActiveChat(chatId, userId);
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