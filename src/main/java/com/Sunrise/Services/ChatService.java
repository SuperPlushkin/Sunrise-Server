package com.Sunrise.Services;

import com.Sunrise.Controllers.ChatController;
import com.Sunrise.DTO.DBResults.ChatMembersPageResult;
import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.Entities.DTO.ChatDTO;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Services.DataServices.DataValidator;
import com.Sunrise.Services.DataServices.LockService;
import com.Sunrise.Subclasses.ValidationException;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;

import static com.Sunrise.Services.DataServices.DataAccessService.randomId;

@Slf4j
@Service
public class ChatService {

    private final DataValidator validator;
    private final DataAccessService dataAccessService;
    private final LockService lockService;

    public ChatService(LockService lockService, DataAccessService dataAccessService, DataValidator validator) {
        this.lockService = lockService;
        this.dataAccessService = dataAccessService;
        this.validator = validator;
    }

    public ChatCreationResult createPersonalChat(@NotNull Long creatorId, @NotNull Long userToAddId) {

        if (creatorId.equals(userToAddId))
            return ChatCreationResult.error("Cannot create personal chat with yourself");

        Set<Long> userIds = Set.of(creatorId, userToAddId);
        long chatId = randomId();

        // WRITE на будущий чат + READ на профили двух пользователей
        if (!lockService.tryLockChatWriteUsersRead(chatId, userIds))
            return ChatCreationResult.error("Try again later");

        try {
            validator.validateActiveUser(creatorId);
            validator.validateActiveUser(userToAddId);

            Optional<ChatDTO> optChat = dataAccessService.getPersonalChat(creatorId, userToAddId);
            if (optChat.isPresent()){
                ChatDTO chat = optChat.get();
                if (chat.isDeleted()) {
                    dataAccessService.restoreChat(chat.getId());
                    log.info("[🔧] ✅ Restored personal chat {} between users {} and {}", chat.getId(), creatorId, userToAddId);
                }
                return ChatCreationResult.success(chat.getId());
            }

            dataAccessService.savePersonalChatAndAddPerson(chatId, creatorId, userToAddId);

            log.info("[🔧] ✅ Created personal chat {} between users {} and {}", chatId, creatorId, userToAddId);
            return ChatCreationResult.success(chatId);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to create personal chat: {}", e.getMessage());
            return ChatCreationResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error creating personal chat: {}", e.getMessage());
            return ChatCreationResult.error("CreatePersonalChat failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUsersRead(chatId, userIds);
        }
    }
    public ChatCreationResult createGroupChat(@NotNull String chatName, @NotNull Long creatorId, @NotNull Set<Long> usersToAddId) {

        if (usersToAddId.contains(creatorId))
            return ChatCreationResult.error("Creator cannot be in usersToAdd list");

        if (usersToAddId.isEmpty())
            return ChatCreationResult.error("Group must have at least one member besides creator");

        Set<Long> allUserIds = new HashSet<>(usersToAddId);
        allUserIds.add(creatorId);

        long chatId = randomId();

        // WRITE на будущий чат + READ на профили всех пользователей
        if (!lockService.tryLockChatWriteUsersRead(chatId, allUserIds))
            return ChatCreationResult.error("Try again later");

        try {
            validator.validateActiveUsers(creatorId, allUserIds);

            dataAccessService.saveGroupChatAndAddPeople(chatId, chatName, creatorId, allUserIds);

            log.info("[🔧] ✅ Created group chat {} '{}' with {} members by creator {}", chatId, chatName, allUserIds.size(), creatorId);
            return ChatCreationResult.success(chatId);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to create group chat: {}", e.getMessage());
            return ChatCreationResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error creating group chat: {}", e.getMessage());
            return ChatCreationResult.error("CreateGroupChat failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUsersRead(chatId, allUserIds);
        }
    }

    public SimpleResult addGroupMember(@NotNull Long chatId, @NotNull Long inviterId, @NotNull Long userToAddId) {

        if (inviterId.equals(userToAddId))
            return SimpleResult.error("Cannot add yourself to the chat");

        Set<Long> users = Set.of(inviterId, userToAddId);

        // WRITE на чат + READ на профиль пригласителя и нового пользователя
        if (!lockService.tryLockChatWriteUsersRead(chatId, users))
            return SimpleResult.error("Try again later");

        try {
            validator.validateAddGroupMember(chatId, inviterId, userToAddId);

            dataAccessService.saveChatMember(chatId, userToAddId);

            log.info("[🔧] ✅ User {} added user {} to group chat {}", inviterId, userToAddId, chatId);
            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to add member to chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error adding member to chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error("AddGroupMember failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUsersRead(chatId, users);
        }
    }
    public SimpleResult leaveChat(@NotNull Long chatId, @NotNull Long userId) {

        // WRITE на чат + READ на профиль пользователя
        if (!lockService.tryLockChatWriteUserRead(chatId, userId))
            return SimpleResult.error("Try again later");

        try {
            ChatDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);

            if (chat.isGroup()) {
                if (chat.isMoreThenOneMember()) {
                    dataAccessService.removeUserFromChat(chatId, userId);
                    log.info("[🔧] ✅ {} {} left group chat {}", userId.equals(chat.getCreatedBy()) ? "Creator" : "User", userId, chatId);
                } else {
                    dataAccessService.deleteChat(chatId);
                    log.info("[🔧] ✅ Last admin {} left group chat {}, chat deleted", userId, chatId);
                }
            }
            else {
                dataAccessService.deleteChat(chatId);
                log.info("[🔧] ✅ User {} deleted personal chat {}", userId, chatId);
            }

            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to leave chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error leaving chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error("LeaveChat failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUserRead(chatId, userId);
        }
    }

    public SimpleResult updateAdminRights(@NotNull Long chatId, @NotNull Long adminId, @NotNull Long userToUpdate, @NotNull Boolean isAdmin) {

        if (adminId.equals(userToUpdate))
            return SimpleResult.error("Cannot add yourself to the chat");

        Set<Long> users = Set.of(adminId, userToUpdate);

        // WRITE на чат + READ на админа и юзера
        if (!lockService.tryLockChatWriteUsersRead(chatId, users))
            return SimpleResult.error("Try again later");

        try {
            validator.validateActiveUsersInActiveChatAndOneIsAdmin(chatId, adminId, userToUpdate);

            dataAccessService.updateAdminRights(chatId, userToUpdate, isAdmin);

            log.info("[🔧] ✅ Updated admin rights for user {} by admin {} in group chat {}", userToUpdate, adminId, chatId);
            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update admin rights for user {} by admin {} in group chat {}: {}", userToUpdate, adminId, chatId, e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating admin rights for user {} by admin {} in group chat {}: {}", userToUpdate, adminId, chatId, e.getMessage());
            return SimpleResult.error("AddGroupMember failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUsersRead(chatId, users);
        }
    }
    public SimpleResult deleteChat(Long chatId, Long userId) {

        // WRITE на чат + READ на профиль пользователя
        if (!lockService.tryLockChatWriteUserRead(chatId, userId))
            return SimpleResult.error("Try again later");

        try {
            validator.validateCanDeleteChat(chatId, userId);

            dataAccessService.deleteChat(chatId);

            log.info("[🔧] ✅ Admin {} deleted chat {}", userId, chatId);
            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to delete chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error("DeleteChat failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUserRead(chatId, userId);
        }
    }

    public HistoryOperationResult deleteAllChatMessages(Long chatId, ChatController.ClearType clearType, Long userId) {

        // WRITE на чат + READ на профиль пользователя
        if (!lockService.tryLockChatWriteUserRead(chatId, userId))
            return HistoryOperationResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            if (clearType.equals(ChatController.ClearType.FOR_ALL)) {
                validator.validateCanClearForAll(chatId, userId);
            }

            var messagesCount = switch (clearType) {
                case FOR_ALL -> dataAccessService.deleteAllChatMessagesForAll(chatId, userId);
                case FOR_SELF -> dataAccessService.deleteAllChatMessagesForSelf(chatId, userId);
            };

            log.info("[🔧] ✅ User {} cleared chat {} history ({} messages) for {}", userId, chatId, messagesCount, clearType);
            return HistoryOperationResult.success(messagesCount);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to clear chat {} history: {}", chatId, e.getMessage());
            return HistoryOperationResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error clearing chat {} history: {}", chatId, e.getMessage());
            return HistoryOperationResult.error("ClearChatHistory failed due to server error");
        }
        finally {
            lockService.unLockChatWriteUserRead(chatId, userId);
        }
    }

    public UserChatsResult getUserChats(Long userId, int offset, int limit) {

        // READ на профиль пользователя
        if (!lockService.tryLockUserRead(userId))
            return UserChatsResult.error("Try again later");

        try {
            validator.validateActiveUser(userId);

            List<ChatDTO> chats = dataAccessService.getUserChatsPage(userId, offset, limit);

            log.debug("[🔧] ✅ User {} has {} chats", userId, chats.size());
            return UserChatsResult.success(chats, chats.size());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get user {} chats: {}", userId, e.getMessage());
            return UserChatsResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting user {} chats: {}", userId, e.getMessage());
            return UserChatsResult.error("getUserChats failed due to server error");
        }
        finally {
            lockService.unLockUserRead(userId);
        }
    }
    public ChatStatsResult getChatStats(Long chatId, Long userId) {

        // READ на чат + READ на профиль пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return ChatStatsResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatStatsDBResult result = dataAccessService.getChatClearStats(chatId, userId);

            log.debug("[🔧] ✅ User {} viewed stats for chat {}", userId, chatId);
            return ChatStatsResult.success(
                result.getTotalMessages(),
                result.getDeletedForAll(),
                result.getDeletedForUser(),
                result.getCanDeleteForAll()
            );
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} stats: {}", chatId, e.getMessage());
            return ChatStatsResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} stats: {}", chatId, e.getMessage());
            return ChatStatsResult.error("GetChatStats failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
    public ChatMembersResult getChatMembers(Long chatId, Long userId) {

        // READ на чат + READ на профиль пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return ChatMembersResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatMembersPageResult chatMembers = dataAccessService.getChatMembersPage(chatId, 0, 20);

            log.debug("[🔧] ✅ User {} viewed {} members of chat {}", userId, chatMembers.chatMembers(), chatId);
            return ChatMembersResult.success(chatMembers.chatMembers(), chatMembers.totalCount());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} members: {}", chatId, e.getMessage());
            return ChatMembersResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} members: {}", chatId, e.getMessage());
            return ChatMembersResult.error("getChatMembers failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
}