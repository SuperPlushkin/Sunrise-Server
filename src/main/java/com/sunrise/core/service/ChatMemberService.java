package com.sunrise.core.service;

import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.DataValidator;
import com.sunrise.core.dataservice.LockManager;
import com.sunrise.core.notifier.WebSocketNotifier;
import com.sunrise.core.service.result.ResultNoArgs;
import com.sunrise.core.service.result.ResultOneArg;
import com.sunrise.entity.pagination.ChatMembersPageDTO;
import com.sunrise.entity.dto.ChatDTO;
import com.sunrise.entity.dto.ChatMemberDTO;
import com.sunrise.helpclass.ValidationException;

import jakarta.validation.constraints.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMemberService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final WebSocketNotifier wsNotify;

    public ResultNoArgs addOrRestoreChatMember(long chatId, long inviterId, long opponentId) {
        try {
            if (inviterId == opponentId) {
                throw new ValidationException("Cannot add yourself to the chat");
            }

            validator.validateAddChatMember(chatId, inviterId, opponentId);

            var chatMember = ChatMemberDTO.create(chatId, opponentId, LocalDateTime.now(), false);

            dataOrchestrator.saveOrRestoreChatMember(chatMember);

            // уведомить всех надо об этом
            wsNotify.notifyChatMemberNew(chatMember);

            log.info("[🔧] ✅ User {} added user {} to group chat {}", inviterId, opponentId, chatId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to add member to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error adding member to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("AddGroupMember failed due to server error");
        }
    }
    public ResultNoArgs addOrRestoreChatMembers(long chatId, long inviterId, @NotNull Map<Long, Boolean> usersToAdd) {
        try {
            if (usersToAdd.containsKey(inviterId)) {
                throw new ValidationException("Cannot add yourself to the chat");
            }

            validator.validateAddChatMembers(chatId, inviterId, usersToAdd.keySet());

            LocalDateTime createdAt = LocalDateTime.now();

            List<ChatMemberDTO> members = new ArrayList<>(usersToAdd.size() + 1);
            members.add(ChatMemberDTO.create(chatId, inviterId, createdAt, true));

            for (Map.Entry<Long, Boolean> entry : usersToAdd.entrySet()){
                members.add(ChatMemberDTO.create(chatId, entry.getKey(), createdAt, entry.getValue()));
            }

            dataOrchestrator.saveOrRestoreChatMembers(chatId, members);

            // уведомить всех надо об этом
            wsNotify.notifyChatMembersNew(members);

            log.info("[🔧] ✅ User {} added users {} to group chat {}", inviterId, members, chatId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to add members to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error adding members to chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("AddGroupMember failed due to server error");
        }
    }

    public ResultNoArgs updateChatMemberInfo(long chatId, long adminId, long userToUpdateId, String tag) {
        try {
            validator.validateActiveUsersInActiveChatAndOneIsAdmin(chatId, adminId, userToUpdateId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateChatMemberInfo(chatId, adminId, tag, updatedAt);

            // уведомить всех надо об этом
            wsNotify.notifyChatMemberInfoUpdated(chatId, adminId, tag, updatedAt);

            log.info("[🔧] ✅ Updated member info for user {} by admin {} chat {}", userToUpdateId, adminId, chatId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update member info for user {} by admin {} in chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating member info for user {} by admin {} in chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error("updateChatMemberInfo failed due to server error");
        }
    }
    public ResultNoArgs updateChatMemberAdminRight(long chatId, long adminId, long userToUpdateId, boolean isAdmin) {
        try {
            if (adminId == userToUpdateId) {
                throw new ValidationException("Cannot update rights of yourself");
            }

            validator.validateActiveUsersInActiveChatAndOneIsAdmin(chatId, adminId, userToUpdateId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateChatMemberAdminRights(chatId, userToUpdateId, isAdmin, updatedAt);

            // уведомить всех надо об этом
            wsNotify.notifyChatMemberAdminRightsUpdated(chatId, userToUpdateId, isAdmin, updatedAt);

            log.info("[🔧] ✅ Updated admin rights for user {} by admin {} in group chat {}", userToUpdateId, adminId, chatId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update admin rights for user {} by admin {} in group chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating admin rights for user {} by admin {} in group chat {}: {}", userToUpdateId, adminId, chatId, e.getMessage());
            return ResultNoArgs.error("updateChatMemberAdminRight failed due to server error");
        }
    }
    public ResultNoArgs updateSelfChatSettings(long chatId, long userId, boolean isPinned) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateChatMemberSetting(chatId, userId, isPinned, updatedAt);

            // уведомить всех надо об этом
            wsNotify.notifySelfChatSettingsUpdated(chatId, userId, isPinned, updatedAt);

            log.info("[🔧] ✅ Updated member info for user {} chat {}", userId, chatId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to update member info for user {} in chat {}: {}", userId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error updating member info for user {} in chat {}: {}", userId, chatId, e.getMessage());
            return ResultNoArgs.error("updateChatMemberInfo failed due to server error");
        }
    }
    public ResultNoArgs kickChatMember(long chatId, long adminId, long userToKickId) {
        try {
            if (adminId == userToKickId) {
                throw new ValidationException("Cannot update rights of yourself");
            }

            if (!lockManager.tryLockLeaveChatOperation(chatId)) {
                throw new ValidationException("Try again later");
            }

            validator.validateActiveUsersInActiveChatAndOneIsAdmin(chatId, adminId, userToKickId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.removeUserFromChat(chatId, userToKickId, updatedAt);

            // уведомить всех надо об этом
            wsNotify.notifyChatMemberDeleted(chatId, userToKickId, updatedAt);

            log.info("[🔧] ✅ User {} kicked from chat {} by user {}", userToKickId, chatId, adminId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to kick user {} from chat {}: {}", userToKickId, chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error kicking user {} from chat {}: {}", userToKickId, chatId, e.getMessage());
            return ResultNoArgs.error("kickChatMember failed due to server error");
        }
        finally {
            lockManager.unLockLeaveChatOperation(chatId);
        }
    }

    public ResultNoArgs leaveChat(long chatId, long userId) {
        try {
            if (!lockManager.tryLockLeaveChatOperation(chatId)) {
                return ResultNoArgs.error("Try again later");
            }

            ChatDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            LocalDateTime updatedAt = LocalDateTime.now();
            if (chat.getChatType().isPersonal()) {
                if (chat.isMoreThenOneMember()) {
                    dataOrchestrator.removeUserFromChat(chatId, userId, updatedAt);
                    log.info("[🔧] ✅ {} {} left group chat {}", (userId == chat.getCreatedBy()) ? "Creator" : "User", userId, chatId);

                    // уведомить всех надо об этом
                    wsNotify.notifyChatMemberDeleted(chatId, userId, updatedAt);
                } else {
                    dataOrchestrator.deleteChat(chatId, updatedAt);
                    log.info("[🔧] ✅ Last admin {} left group chat {}, chat deleted", userId, chatId);

                    // уведомить всех надо об этом
                    wsNotify.notifyChatDeleted(chatId, updatedAt);
                }
            } else {
                dataOrchestrator.deleteChat(chatId, updatedAt);
                log.info("[🔧] ✅ User {} deleted personal chat {}", userId, chatId);

                // уведомить всех надо об этом
                wsNotify.notifyChatDeleted(chatId, updatedAt);
            }

            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to leave chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error leaving chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("LeaveChat failed due to server error");
        }
        finally {
            lockManager.unLockLeaveChatOperation(chatId);
        }
    }

    public ResultOneArg<ChatMembersPageDTO> getChatMembersPage(long chatId, long userId, Long cursor, int limit) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatMembersPageDTO chatMembers = dataOrchestrator.getChatMembersPage(chatId, cursor, limit);

            log.debug("[🔧] ✅ User {} got {} members of chat {}", userId, chatMembers.chatMembers().size(), chatId);
            return ResultOneArg.success(chatMembers);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} members: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} members: {}", chatId, e.getMessage());
            return ResultOneArg.error("getChatMembers failed due to server error");
        }
    }
}