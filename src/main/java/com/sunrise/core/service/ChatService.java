package com.sunrise.core.service;

import com.sunrise.core.service.result.*;
import com.sunrise.entity.dto.ChatMembersPageDTO;
import com.sunrise.core.dataservice.type.ChatStatsDBResult;
import com.sunrise.entity.dto.UserChatsPageDTO;
import com.sunrise.entity.dto.LightChatDTO;
import com.sunrise.entity.dto.LightChatMemberDTO;
import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.DataValidator;
import com.sunrise.core.dataservice.LockManager;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class ChatService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatCreationResult createPersonalChat(long creatorId, long opponentId) {

        if (creatorId == opponentId)
            return ChatCreationResult.error("Cannot create personal chat with yourself");

        // WRITE на будущий чат
        if (!lockManager.tryLockPersonalChatCreation(creatorId, opponentId))
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

            long chatId = SimpleSnowflakeId.nextId();
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
            lockManager.unLockPersonalChatCreation(creatorId, opponentId);
        }
    }
    public ChatCreationResult createGroupChat(long creatorId, @NotNull String chatName, @NotNull Map<Long, Boolean> usersToAdd) {
        try {
            if (usersToAdd.containsKey(creatorId)) {
                throw new ValidationException("Creator cannot be in usersToAdd list");
            }

            validator.validateActiveUsers(creatorId, usersToAdd.keySet());

            long chatId = SimpleSnowflakeId.nextId();
            var chat = LightChatDTO.createGroup(chatId, chatName, creatorId);

            List<LightChatMemberDTO> chatMembers = new ArrayList<>(usersToAdd.size() + 1);
            chatMembers.add(LightChatMemberDTO.create(chatId, creatorId, true));  // creator с правами админа

            for (Map.Entry<Long, Boolean> entry : usersToAdd.entrySet()){
                var chatMember = LightChatMemberDTO.create(chatId, entry.getKey(), entry.getValue());  // остальные без прав
                chatMembers.add(chatMember);
            }

            dataOrchestrator.saveGroupChatAndAddPeople(chat, chatMembers);

            log.info("[🔧] ✅ Created group chat {} '{}' with {} members by creator {}", chatId, chatName, usersToAdd.size(), creatorId);
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
    }

    public SimpleResult addGroupMember(long chatId, long inviterId, long opponentId) {
        try {
            if (inviterId == opponentId) {
                throw new ValidationException("Cannot add yourself to the chat");
            }

            validator.validateAddGroupMember(chatId, inviterId, opponentId);

            dataOrchestrator.saveOrRestoreChatMember(LightChatMemberDTO.create(chatId, opponentId, false));

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
    }
    public SimpleResult addGroupMembers(long chatId, long inviterId, @NotNull Map<Long, Boolean> usersToAdd) {
        try {
            if (usersToAdd.containsKey(inviterId)) {
                throw new ValidationException("Cannot add yourself to the chat");
            }

            validator.validateAddGroupMembers(chatId, inviterId, usersToAdd.keySet());

            List<LightChatMemberDTO> members = new ArrayList<>(usersToAdd.size() + 1);
            members.add(LightChatMemberDTO.create(chatId, inviterId, true));

            for (Map.Entry<Long, Boolean> entry : usersToAdd.entrySet()){
                members.add(LightChatMemberDTO.create(chatId, entry.getKey(), entry.getValue()));
            }

            dataOrchestrator.saveChatMembers(chatId, members);

            log.info("[🔧] ✅ User {} added users {} to group chat {}", inviterId, members, chatId);
            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to add members to chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error adding members to chat {}: {}", chatId, e.getMessage());
            return SimpleResult.error("AddGroupMember failed due to server error");
        }
    }

    public SimpleResult updateAdminRights(long chatId, long adminId, long userToUpdate, boolean isAdmin) {
        try {
            if (adminId == userToUpdate) {
                throw new ValidationException("Cannot update rights of yourself");
            }

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
    }

    public SimpleResult leaveChat(long chatId, long userId) {

        // WRITE на чат
        if (!lockManager.tryLockLeaveChatOperation(chatId))
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
            lockManager.unLockLeaveChatOperation(chatId);
        }
    }
    public SimpleResult deleteChat(long chatId, long userId) {
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
    }

    public UserChatsResult getUserChatsPage(long userId, Long cursor, int limit) {
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
    }
    public ChatMembersResult getChatMembersPage(long chatId, long userId, Long cursor, int limit) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatMembersPageDTO chatMembers = dataOrchestrator.getChatMembersPage(chatId, cursor, limit);

            log.debug("[🔧] ✅ User {} got {} members of chat {}", userId, chatMembers.chatMembers().size(), chatId);
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
    }

    public ChatStatsResult getChatStats(long chatId, long userId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatStatsDBResult result = dataOrchestrator.getChatClearStats(chatId, userId);

            log.debug("[🔧] ✅ User {} viewed stats for chat {}", userId, chatId);
            return ChatStatsResult.success(
                    result.getTotalMessages(),
                    result.getDeletedForAll(),
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
    }
}