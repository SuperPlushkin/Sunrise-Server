package com.Sunrise.Services.DataServices;

import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.Entities.DB.User;
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

    public void validateActiveUser(Long userId) {
        Optional<User> user = dataAccessService.getUser(userId);
        if (user.isEmpty()) {
            throw new ValidationException("User not found: " + userId);
        }

        User u = user.get();
        if (u.isDeleted()) {
            throw new ValidationException("User is deleted: " + userId);
        }
        if (!u.isEnabled()) {
            throw new ValidationException("User is not verified: " + userId);
        }
    }

    public void validateActiveUserInChat(Long chatId, Long userId) {
        validateChatExists(chatId);
        validateActiveUser(userId);
        validateUserInChat(chatId, userId);
    }
    public Boolean validateUserInChatAndGetIsGroup(Long chatId, Long userId) {
        Boolean isGroup = validateChatExistsAndGetIsGroup(chatId);

        validateActiveUser(userId);
        validateUserInChat(chatId, userId);

        return isGroup;
    }
    public Boolean validateUsersInChatAndGetIsGroup(Long chatId, Set<Long> userIds) {
        // Сначала проверяем существование чата
        Boolean isGroup = validateChatExistsAndGetIsGroup(chatId);

        for (Long userId : userIds) {
            validateActiveUser(userId);
            validateUserInChat(chatId, userId);
        }

        return isGroup;
    }

    public void validateAddGroupMember(Long chatId, Long inviterId, Long newUserId) {
        // Проверяем, что это групповой чат
        Boolean isGroup = validateChatExistsAndGetIsGroup(chatId);
        if (!isGroup)
            throw new ValidationException("Cannot add members to personal chat");

        // Проверяем, что приглашающий - админ
        Optional<Boolean> isAdmin = dataAccessService.isChatAdmin(chatId, inviterId);
        if (isAdmin.isEmpty()) {
            throw new ValidationException("Inviter is not a member of this chat");
        }
        if (!isAdmin.get()) {
            throw new ValidationException("Only admin can add members to group");
        }

        // Проверяем нового пользователя
        validateActiveUser(newUserId);

        // Проверяем, что его еще нет в чате
        if (dataAccessService.hasChatMember(chatId, newUserId))
            throw new ValidationException("User is already a member of this group");
    }
    public void validateCanClearForAll(Long chatId, Long userId) {
        validateActiveUserInChat(chatId, userId);

        ChatStatsDBResult stats = dataAccessService.getChatClearStats(chatId, userId);
        if (!stats.getCanClearForAll())
            throw new ValidationException("User does not have permission to clear chat for all");
    }


    // ========== PRIVATE METHODS ==========

    private void validateChatExists(Long chatId) {
        if (!dataAccessService.ensureChatIsValid(chatId))
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
    }
    private Boolean validateChatExistsAndGetIsGroup(Long chatId) {
        Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);
        if (isGroup.isEmpty()) {
            throw new ValidationException("Chat does not exist or is deleted: " + chatId);
        }
        return isGroup.get();
    }
    private void validateUserInChat(Long chatId, Long userId) {
        if (!dataAccessService.hasChatMember(chatId, userId))
            throw new ValidationException("User " + userId + " is not a member of chat " + chatId);
    }
}