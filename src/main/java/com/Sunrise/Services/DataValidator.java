package com.Sunrise.Services;

import com.Sunrise.Entities.User;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Subclasses.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@AllArgsConstructor
public class DataValidator {

    private final DataAccessService dataAccessService;

    public Boolean validateUsersInChatAndGetIsGroup(Long chatId, Set<Long> userIds) {
        Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);
        if (isGroup.isEmpty())
            throw new ValidationException("Chat does not exist or delete");

        for (Long userId : userIds){
            validateActiveUser(userId);

            if (!dataAccessService.isUserInChat(chatId, userId))
                throw new ValidationException("User is not a member of this chat");
        }

        return isGroup.get();
    }
    public Boolean validateUserInChatAndGetIsGroup(Long chatId, Long userId) {
        Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);
        if (isGroup.isEmpty())
            throw new ValidationException("Chat does not exist or delete");

        validateActiveUser(userId);

        if (!dataAccessService.isUserInChat(chatId, userId))
            throw new ValidationException("User is not a member of this chat");

        return isGroup.get();
    }

    public void validateActiveUserInChat(Long chatId, Long userId) {
        if (!dataAccessService.chatIsValid(chatId))
            throw new ValidationException("Chat does not exist or delete");

        validateActiveUser(userId);

        if (!dataAccessService.isUserInChat(chatId, userId))
            throw new ValidationException("User is not a member of this chat");
    }
    public void validateActiveUser(Long userId) {
        Optional<User> user = dataAccessService.getUser(userId);
        if (user.isEmpty() || user.get().getIsDeleted() || !user.get().getIsEnabled())
            throw new ValidationException("User is not active");
    }

    public void validateAddGroupMember(Long chatId, Long inviterId, Long newUserId) {
        Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

        if (isGroup.isEmpty())
            throw new ValidationException("Chat not found");

        if (!isGroup.get())
            throw new ValidationException("Cannot add members to personal chat");

        var isChatAdmin = dataAccessService.isChatAdmin(chatId, inviterId);

        if (isChatAdmin.isEmpty())
            throw new ValidationException("User not found");

        if (!isChatAdmin.get())
            throw new ValidationException("Only admin can add members to group");

        validateActiveUser(newUserId);

        if (dataAccessService.isUserInChat(chatId, newUserId))
            throw new ValidationException("User is already a member of this group");
    }
}
