package com.Sunrise.Core.Services;

import com.Sunrise.DTOs.Paginations.ChatMembersPageDTO;
import com.Sunrise.DTOs.DBResults.ChatStatsDBResult;
import com.Sunrise.DTOs.Paginations.UserChatsPageDTO;
import com.Sunrise.Entities.DTOs.LightChatDTO;
import com.Sunrise.DTOs.ServiceResults.*;
import com.Sunrise.Entities.DTOs.LightChatMemberDTO;
import com.Sunrise.Core.DataServices.DataOrchestrator;
import com.Sunrise.Core.DataServices.DataValidator;
import com.Sunrise.Core.DataServices.LockManager;
import com.Sunrise.Subclasses.SimpleSnowflakeId;
import com.Sunrise.Subclasses.ValidationException;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ChatService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;

    public ChatService(LockManager lockManager, DataOrchestrator dataOrchestrator, DataValidator validator) {
        this.lockManager = lockManager;
        this.dataOrchestrator = dataOrchestrator;
        this.validator = validator;
    }

    public ChatCreationResult createPersonalChat(long creatorId, long opponentId) {

        if (creatorId == opponentId)
            return ChatCreationResult.error("Cannot create personal chat with yourself");

        Set<Long> userIds = Set.of(creatorId, opponentId);
        long chatId = SimpleSnowflakeId.nextId();

        // WRITE на будущий чат + READ на профили двух пользователей
        if (!lockManager.tryLockChatWriteUsersRead(chatId, userIds))
            return ChatCreationResult.error("Try again later");

        try {
            validator.validateActiveUser(creatorId);
            validator.validateActiveUser(opponentId);

            Optional<LightChatDTO> optChat = dataOrchestrator.getPersonalChat(creatorId, opponentId);
            LightChatDTO chat;
            if (optChat.isPresent()){
                chat = optChat.get();
                if (chat.isDeleted()) {
                    dataOrchestrator.restoreChat(chat.getId());
                    log.info("[🔧] ✅ Restored personal chat {} between users {} and {}", chat.getId(), creatorId, opponentId);
                }
                return ChatCreationResult.success(chat.getId());
            }

            chat = LightChatDTO.createPrivate(chatId, creatorId, opponentId);

            var creator = LightChatMemberDTO.create(chatId, creatorId, false);
            var opponent = LightChatMemberDTO.create(chatId, opponentId, false);

            dataOrchestrator.savePersonalChatAndAddPerson(chat, creator, opponent);

            log.info("[🔧] ✅ Created personal chat {} between users {} and {}", chatId, creatorId, opponentId);
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
            lockManager.unLockChatWriteUsersRead(chatId, userIds);
        }
    }
    public ChatCreationResult createGroupChat(@NotNull String chatName, long creatorId, @NotNull Set<Long> userToAddIds) {

        if (userToAddIds.contains(creatorId))
            return ChatCreationResult.error("Creator cannot be in usersToAdd list");

        if (userToAddIds.isEmpty())
            return ChatCreationResult.error("Group must have at least one member besides creator");

        Set<Long> allUserIds = new HashSet<>(userToAddIds);
        allUserIds.add(creatorId);

        long chatId = SimpleSnowflakeId.nextId();

        // WRITE на будущий чат + READ на профили всех пользователей
        if (!lockManager.tryLockChatWriteUsersRead(chatId, allUserIds))
            return ChatCreationResult.error("Try again later");

        try {
            validator.validateActiveUsers(creatorId, allUserIds);

            var chat = LightChatDTO.createGroup(chatId, chatName, creatorId);

            List<LightChatMemberDTO> chatMembers = new ArrayList<>(userToAddIds.size());
            chatMembers.add(LightChatMemberDTO.create(chatId, creatorId, true));  // creator с правами админа

            for (long userId : userToAddIds){
                var chatMember = LightChatMemberDTO.create(chatId, userId, false);  // остальные без прав
                chatMembers.add(chatMember);
            }

            dataOrchestrator.saveGroupChatAndAddPeople(chat, chatMembers);

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
            lockManager.unLockChatWriteUsersRead(chatId, allUserIds);
        }
    }

    public SimpleResult addGroupMember(long chatId, long inviterId, long opponentId) {

        if (inviterId == opponentId)
            return SimpleResult.error("Cannot add yourself to the chat");

        Set<Long> users = Set.of(inviterId, opponentId);

        // WRITE на чат + READ на профиль пригласителя и нового пользователя
        if (!lockManager.tryLockChatWriteUsersRead(chatId, users))
            return SimpleResult.error("Try again later");

        try {
            validator.validateAddGroupMember(chatId, inviterId, opponentId);

            dataOrchestrator.saveChatMember(LightChatMemberDTO.create(chatId, opponentId, false));

            log.info("[🔧] ✅ User {} added user {} to group chat {}", inviterId, opponentId, chatId);
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
            lockManager.unLockChatWriteUsersRead(chatId, users);
        }
    }
    public SimpleResult leaveChat(long chatId, long userId) {

        // WRITE на чат + READ на профиль пользователя
        if (!lockManager.tryLockChatWriteUserRead(chatId, userId))
            return SimpleResult.error("Try again later");

        try {
            LightChatDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            if (chat.isGroup()) {
                if (chat.isMoreThenOneMember()) {
                    dataOrchestrator.removeUserFromChat(chatId, userId);
                    log.info("[🔧] ✅ {} {} left group chat {}", (userId == chat.getCreatedBy()) ? "Creator" : "User", userId, chatId);
                } else {
                    dataOrchestrator.deleteChat(chatId);
                    log.info("[🔧] ✅ Last admin {} left group chat {}, chat deleted", userId, chatId);
                }
            }
            else {
                dataOrchestrator.deleteChat(chatId);
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
            lockManager.unLockChatWriteUserRead(chatId, userId);
        }
    }

    public SimpleResult updateAdminRights(long chatId, long adminId, long userToUpdate, boolean isAdmin) {

        if (adminId == userToUpdate)
            return SimpleResult.error("Cannot add yourself to the chat");

        Set<Long> users = Set.of(adminId, userToUpdate);

        // WRITE на чат + READ на админа и юзера
        if (!lockManager.tryLockChatWriteUsersRead(chatId, users))
            return SimpleResult.error("Try again later");

        try {
            validator.validateActiveUsersInActiveChatAndOneIsAdmin(chatId, adminId, userToUpdate);

            dataOrchestrator.updateAdminRights(chatId, userToUpdate, isAdmin);

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
            lockManager.unLockChatWriteUsersRead(chatId, users);
        }
    }
    public SimpleResult deleteChat(long chatId, long userId) {

        // WRITE на чат + READ на профиль пользователя
        if (!lockManager.tryLockChatWriteUserRead(chatId, userId))
            return SimpleResult.error("Try again later");

        try {
            validator.validateCanDeleteChat(chatId, userId);

            dataOrchestrator.deleteChat(chatId);

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
            lockManager.unLockChatWriteUserRead(chatId, userId);
        }
    }

    public UserChatsResult getUserChats(long userId, Long cursor, int limit) {

        // READ на профиль пользователя
        if (!lockManager.tryLockUserRead(userId))
            return UserChatsResult.error("Try again later");

        try {
            validator.validateActiveUser(userId);

            UserChatsPageDTO chats = dataOrchestrator.getUserChatsPage(userId, cursor, limit);

            log.debug("[🔧] ✅ User {} got {} chats", userId, chats.chats().size());
            return UserChatsResult.success(chats);
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
            lockManager.unLockUserRead(userId);
        }
    }
    public ChatMembersResult getChatMembers(long chatId, Long cursor, long userId) {

        // READ на чат + READ на профиль пользователя
        if (!lockManager.tryLockChatReadUserRead(chatId, userId))
            return ChatMembersResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatMembersPageDTO chatMembers = dataOrchestrator.getChatMembersPage(chatId, cursor, 20);

            log.debug("[🔧] ✅ User {} viewed {} members of chat {}", userId, chatMembers.chatMembers(), chatId);
            return ChatMembersResult.success(chatMembers);
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
            lockManager.unLockChatReadUserRead(chatId, userId);
        }
    }

    public ChatStatsResult getChatStats(long chatId, long userId) {

        // READ на чат + READ на профиль пользователя
        if (!lockManager.tryLockChatReadUserRead(chatId, userId))
            return ChatStatsResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatStatsDBResult result = dataOrchestrator.getChatClearStats(chatId, userId);

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
            lockManager.unLockChatReadUserRead(chatId, userId);
        }
    }
}