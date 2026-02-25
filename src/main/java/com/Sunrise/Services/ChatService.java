package com.Sunrise.Services;

import com.Sunrise.Controllers.ChatController;
import com.Sunrise.DTO.DBResults.ChatStatsDBResult;
import com.Sunrise.DTO.Responses.ChatMemberDTO;
import com.Sunrise.DTO.Responses.ChatDTO;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Entities.DB.Chat;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Services.DataServices.DataValidator;
import com.Sunrise.Services.DataServices.LockService;
import com.Sunrise.Subclasses.ValidationException;

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

    public ChatCreationResult createPersonalChat(Long creatorId, Long userToAddId) {

        if (creatorId.equals(userToAddId))
            return ChatCreationResult.error("Cannot create personal chat with yourself");

        Set<Long> usersId = Set.of(creatorId, userToAddId);

        lockService.lockGlobalChats();
        lockService.lockUsersSafely(usersId, true);
        try
        {
            validator.validateActiveUser(creatorId);
            validator.validateActiveUser(userToAddId);

            Optional<Chat> optChat = dataAccessService.getPersonalChat(creatorId, userToAddId);
            if (optChat.isPresent()){
                Chat chat = optChat.get();
                if (chat.isDeleted()) {
                    dataAccessService.restoreChat(chat.getId());
                    log.info("[🔧] ✅ Restored personal chat {} between users {} and {}", chat.getId(), creatorId, userToAddId);
                }
                return ChatCreationResult.success(chat.getId());
            }

            Chat chat = Chat.createPersonalChat(randomId(), creatorId);

            var creator = new ChatMember(chat.getId(), creatorId, true);
            var member = new ChatMember(chat.getId(), userToAddId, false);

            dataAccessService.savePersonalChatAndAddPerson(chat, creator, member);

            log.info("[🔧] ✅ Created personal chat {} between users {} and {}", chat.getId(), creatorId, userToAddId);
            return ChatCreationResult.success(chat.getId());
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
            lockService.unlockUsersSafely(usersId, true);
            lockService.unlockGlobalChats();
        }
    } // TODO: НАДО ПОДУМАТЬ С УДАЛЕНИЕМ ЛИЧНЫХ ЧАТОВ
    public ChatCreationResult createGroupChat(String chatName, Long creatorId, Set<Long> usersToAddId) {

        if (usersToAddId.contains(creatorId))
            return ChatCreationResult.error("Creator cannot be in usersToAdd list");

        if (usersToAddId.isEmpty())
            return ChatCreationResult.error("Group must have at least one member besides creator");

        Set<Long> allUserIds = new HashSet<>(usersToAddId);
        allUserIds.add(creatorId);

        lockService.lockGlobalChats();
        lockService.lockUsersSafely(allUserIds, false);
        try
        {
            validator.validateActiveUser(creatorId);

            int membersCount = usersToAddId.size() + 1;

            Chat chat = Chat.createGroupChat(randomId(), chatName, membersCount, creatorId);

            List<ChatMember> members = new ArrayList<>(membersCount);
            members.add(new ChatMember(chat.getId(), creatorId, true));

            for (Long userToAddId : usersToAddId) {
                validator.validateActiveUser(userToAddId);
                members.add(new ChatMember(chat.getId(), userToAddId, false));
            }

            dataAccessService.saveGroupChatAndAddPeople(chat, members);

            log.info("[🔧] ✅ Created group chat {} '{}' with {} members by creator {}", chat.getId(), chatName, membersCount, creatorId);
            return ChatCreationResult.success(chat.getId());
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
            lockService.unlockUsersSafely(allUserIds, false);
            lockService.unlockGlobalChats();
        }
    } // TODO: ПЕРЕДЕЛАТЬ LOCKs И ТАКЖЕ ЕЩЕ ПОДУМАТЬ НАД ГРУППОЙ С ОДНИМ УЧАСТНИКОМ

    public SimpleResult addGroupMember(Long chatId, Long inviterId, Long userToAddId) {

        if (inviterId.equals(userToAddId))
            return SimpleResult.error("Cannot add yourself to the chat");

        lockService.lockWriteChat(chatId);
        try
        {
            validator.validateAddGroupMember(chatId, inviterId, userToAddId);

            ChatMember chatMember = new ChatMember(chatId, userToAddId, false);
            dataAccessService.saveChatMember(chatMember);

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
            lockService.unlockWriteChat(chatId);
        }
    }
    public SimpleResult leaveChat(Long chatId, Long userId) {

        lockService.lockWriteChat(chatId);
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);
            if (isGroup.isEmpty())
                throw new ValidationException("Chat not found");

            if (isGroup.get())
            {
                Optional<Long> creatorId = dataAccessService.getChatCreator(chatId);
                if (creatorId.isEmpty())
                    throw new ValidationException("Chat not found");

                if (userId.equals(creatorId.get())) {
                    Optional<Long> anotherAdmin = dataAccessService.findAnotherAdmin(chatId, userId);

                    if (anotherAdmin.isPresent()) {
                        dataAccessService.updateChatCreator(chatId, anotherAdmin.get());
                        dataAccessService.removeUserFromChat(chatId, userId);
                        log.info("[🔧] ✅ Creator {} left group chat {}, transferred ownership to {}", userId, chatId, anotherAdmin.get());
                    } else {
                        dataAccessService.deleteChat(chatId);
                        log.info("[🔧] ✅ Last admin {} left group chat {}, chat deleted", userId, chatId);
                    }
                } else {
                    dataAccessService.removeUserFromChat(chatId, userId);
                    log.info("[🔧] ✅ User {} left group chat {}", userId, chatId);
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
            lockService.unlockWriteChat(chatId);
        }
    }

    public HistoryOperationResult clearChatHistory(Long chatId, ChatController.ClearType clearType, Long userId) {

        lockService.lockWriteChat(chatId);
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            if (clearType == ChatController.ClearType.FOR_ALL) {
                validator.validateCanClearForAll(chatId, userId);
            }

            var messagesCount = switch (clearType) {
                case FOR_ALL -> dataAccessService.clearChatHistoryForAll(chatId, userId);
                case FOR_SELF -> dataAccessService.clearChatHistoryForSelf(chatId, userId);
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
            lockService.unlockWriteChat(chatId);
        }
    }

    public UserChatsResult getUserChats(Long userId) {

        lockService.lockGlobalChats();
        try
        {
            validator.validateActiveUser(userId);

            List<ChatDTO> chats = dataAccessService.getUserChats(userId);

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
            lockService.unlockGlobalChats();
        }
    }
    public ChatStatsResult getChatStats(Long chatId, Long userId) {

        lockService.lockReadChat(chatId);
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            ChatStatsDBResult result = dataAccessService.getChatClearStats(chatId, userId);

            log.debug("[🔧] ✅ User {} viewed stats for chat {}", userId, chatId);
            return ChatStatsResult.success(
                result.getTotalMessages(),
                result.getDeletedForAll(),
                result.getHiddenByUser(),
                result.getCanClearForAll()
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
            lockService.unlockReadChat(chatId);
        }
    }
    public ChatMembersResult getChatMembers(Long chatId, Long userId) {

        lockService.lockReadChat(chatId);
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            List<ChatMemberDTO> chatMembers = dataAccessService.getChatMembersPage(chatId, 0, 0);

            log.debug("[🔧] ✅ User {} viewed {} members of chat {}", userId, chatMembers.size(), chatId);
            return ChatMembersResult.success(chatMembers, chatMembers.size());
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
            lockService.unlockReadChat(chatId);
        }
    }
}