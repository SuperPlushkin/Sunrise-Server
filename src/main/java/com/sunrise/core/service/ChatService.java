package com.sunrise.core.service;

import com.sunrise.entity.dto.FullChatDTO;
import com.sunrise.entity.pagination.UserChatsPageDTO;
import com.sunrise.entity.dto.LightChatDTO;
import com.sunrise.entity.dto.ChatMemberDTO;

import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.DataValidator;
import com.sunrise.core.dataservice.LockManager;
import com.sunrise.core.dataservice.type.ChatType;
import com.sunrise.core.dataservice.type.ChatStatsDBResult;
import com.sunrise.core.notifier.WebSocketNotifier;
import com.sunrise.core.service.result.*;

import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;
    private final WebSocketNotifier wsNotify;

    public ResultOneArg<Long> createPersonalChat(long tempId, long creatorId, long opponentId) {

        if (creatorId == opponentId)
            return ResultOneArg.error("Cannot create personal chat with yourself");

        // WRITE на будущий чат
        if (!lockManager.tryLockPersonalChatCreation(creatorId, opponentId))
            return ResultOneArg.error("Try again later");

        try {
            validator.validateActiveUser(creatorId);
            validator.validateActiveUser(opponentId);

            LocalDateTime createdAt = LocalDateTime.now();
            Optional<LightChatDTO> optChat = dataOrchestrator.getPersonalChat(creatorId, opponentId);
            if (optChat.isPresent()){
                LightChatDTO chat = optChat.get();
                long chatId = chat.getId();
                if (chat.isDeleted()) {
                    dataOrchestrator.restoreChat(chatId, createdAt);
                    log.info("[🔧] ✅ Restored personal chat {} between users {} and {}", chatId, creatorId, opponentId);
                }
                return ResultOneArg.success(chatId);
            }

            long chatId = SimpleSnowflakeId.nextId();

            LightChatDTO chat = LightChatDTO.createPersonal(chatId, opponentId, createdAt, creatorId);

            var creator = ChatMemberDTO.create(chatId, creatorId, createdAt, false);
            var opponent = ChatMemberDTO.create(chatId, opponentId, createdAt, false);

            dataOrchestrator.savePersonalChatAndAddMembers(chat, creator, opponent);

            // уведомить надо
            wsNotify.notifyChatNew(tempId, chat, Set.of(creatorId, opponentId));

            log.info("[🔧] ✅ Created personal chat {} between users {} and {}", chatId, creatorId, opponentId);
            return ResultOneArg.success(chatId);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to create personal chat: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error creating personal chat: {}", e.getMessage());
            return ResultOneArg.error("CreatePersonalChat failed due to server error");
        }
        finally {
            lockManager.unLockPersonalChatCreation(creatorId, opponentId);
        }
    }
    public ResultOneArg<Long> createGroupChat(long tempId, long creatorId, @NotNull String chatName, @NotNull String chatDescription, @NotNull ChatType chatType, @NotNull Map<Long, Boolean> usersToAdd) {
        try {
            if (chatType.isPersonal()) {
                throw new ValidationException("Cannot create group if chatType is personal");
            }

            if (usersToAdd.containsKey(creatorId)) {
                throw new ValidationException("Creator cannot be in usersToAdd list");
            }

            int membersCount = usersToAdd.size() + 1;
            if (chatType.isMembersInBound(membersCount)) {
                throw new ValidationException("Members not in bound of chatType --> " + chatType);
            }

            validator.validateActiveUsers(creatorId, usersToAdd.keySet());

            long chatId = SimpleSnowflakeId.nextId();
            LocalDateTime createdAt = LocalDateTime.now();

            LightChatDTO chat = LightChatDTO.createGroup(chatId, chatName, chatDescription, chatType, membersCount, createdAt, creatorId);

            var usersToNotify = new HashSet<>(usersToAdd.keySet());
            List<ChatMemberDTO> chatMembers = new ArrayList<>(membersCount);
            chatMembers.add(ChatMemberDTO.create(chatId, creatorId, createdAt, true));  // creator с правами админа
            usersToNotify.add(creatorId);

            for (Map.Entry<Long, Boolean> entry : usersToAdd.entrySet()){
                var chatMember = ChatMemberDTO.create(chatId, entry.getKey(), createdAt, entry.getValue());  // остальные с правами хэш таблицы
                chatMembers.add(chatMember);
                usersToNotify.add(entry.getKey());
            }

            dataOrchestrator.saveGroupChatAndAddMembers(chat, chatMembers);

            // уведомить надо
            wsNotify.notifyChatNew(tempId, chat, usersToNotify);

            log.info("[🔧] ✅ Created group chat {} '{}' with {} members by creator {}", chatId, chatName, usersToAdd.size(), creatorId);
            return ResultOneArg.success(chatId);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to create group chat: {}", e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error creating group chat: {}", e.getMessage());
            return ResultOneArg.error("CreateGroupChat failed due to server error");
        }
    }

    public ResultNoArgs updateChatInfo(long chatId, long userId, String newName, String newDescription) {
        try {
            validator.validateCanUpdateChatInfo(chatId, userId);

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateChatInfo(chatId, newName, newDescription, updatedAt);

            // уведомить надо
            wsNotify.notifyChatInfoUpdated(chatId, newName, newDescription, updatedAt);

            log.info("[🔧] ✅ Chat info changed for chat {} by user {}", chatId, userId);
            return ResultNoArgs.success();
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to change chat info {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error changing chat info {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("ChangeChatInfo failed due to server error");
        }
    }
    public ResultNoArgs updateChatType(long chatId, long userId, ChatType newType) {
        try {
            LightChatDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            if (chat.isPersonal()) {
                throw new ValidationException("Cannot change type of personal chat");
            }
            if (!chat.isChangeable()) {
                throw new ValidationException("This chat type cannot be changed");
            }

            if (newType.isPersonal()) {
                throw new ValidationException("Cannot change group chat to personal");
            }
            if (!newType.isChangeable()) {
                throw new ValidationException("Invalid target type");
            }

            // Права: только администратор
            Optional<Boolean> isAdmin = dataOrchestrator.isActiveAdminInActiveChat(chatId, userId);
            if (isAdmin.isEmpty() || !isAdmin.get()) {
                throw new ValidationException("Only admin can change chat type");
            }

            LocalDateTime updatedAt = LocalDateTime.now();
            dataOrchestrator.updateChatType(chatId, newType, updatedAt);

            // уведомить надо
            wsNotify.notifyChatTypeUpdated(chatId, newType, updatedAt);

            log.info("[🔧] ✅ Chat {} type changed to {} by user {}", chatId, newType, userId);
            return ResultNoArgs.success();
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to change chat type {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error changing chat type {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("ChangeChatType failed due to server error");
        }
    }

    public ResultNoArgs deleteChat(long chatId, long userId) {
        try {
            validator.validateCanDeleteChat(chatId, userId);

            LocalDateTime deletedAt = LocalDateTime.now();
            dataOrchestrator.deleteChat(chatId, deletedAt);

            // уведомить надо
            wsNotify.notifyChatDeleted(chatId, deletedAt);

            log.info("[🔧] ✅ Admin {} deleted chat {}", userId, chatId);
            return ResultNoArgs.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to delete chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting chat {}: {}", chatId, e.getMessage());
            return ResultNoArgs.error("DeleteChat failed due to server error");
        }
    }

    public ResultOneArg<FullChatDTO> getUserChat(long chatId, long userId) {
        try {
            validator.validateActiveUser(userId);

            FullChatDTO chat = dataOrchestrator.getUserChat(chatId, userId)
                    .orElseThrow(() -> new ValidationException("Chat is deleted or not found"));

            log.debug("[🔧] ✅ User {} got chat {}", userId, chatId);
            return ResultOneArg.success(chat);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get user {} chat {}: {}", userId, chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting user {} chat {}: {}", userId, chatId, e.getMessage());
            return ResultOneArg.error("getUserChat failed due to server error");
        }
    }
    public ResultOneArg<UserChatsPageDTO> getUserChatsPage(long userId, Boolean isPinnedCursor, Long lastMsgIdCursor, Long chatIdCursor, int limit) {
        try {
            validator.validateActiveUser(userId);

            UserChatsPageDTO chats = dataOrchestrator.getUserChatsPage(userId, isPinnedCursor, lastMsgIdCursor, chatIdCursor, limit);

            log.debug("[🔧] ✅ User {} got {} chats", userId, chats.chats().size());
            return ResultOneArg.success(chats);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get user {} chats: {}", userId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting user {} chats: {}", userId, e.getMessage());
            return ResultOneArg.error("getUserChatsPage failed due to server error");
        }
    }
    public ResultOneArg<ChatStatsResult> getChatStats(long chatId, long userId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            ChatStatsDBResult result = dataOrchestrator.getChatClearStats(chatId, userId);

            log.debug("[🔧] ✅ User {} viewed stats for chat {}", userId, chatId);
            return ResultOneArg.success(
                new ChatStatsResult(
                    result.getTotalMessages(),
                    result.getDeletedForAll(),
                    result.getCanDeleteForAll()
                )
            );
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get chat {} stats: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting chat {} stats: {}", chatId, e.getMessage());
            return ResultOneArg.error("GetChatStats failed due to server error");
        }
    }
    public ResultOneArg<Boolean> isActionsEnabledForChat(long chatId, long userId) {
        try {
            LightChatDTO chat = validator.validateActiveUserInActiveChatAndGetChat(chatId, userId);
            return ResultOneArg.success(chat.isActionsEnabled());
        } catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed to get enabled actions for chat {}: {}", chatId, e.getMessage());
            return ResultOneArg.error(e.getMessage());
        } catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting enabled actions for chat {}: {}", chatId, e.getMessage());
            return ResultOneArg.error("isActionsEnabledForChat failed due to server error");
        }
    }
}