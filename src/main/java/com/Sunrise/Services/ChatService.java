package com.Sunrise.Services;

import com.Sunrise.Controllers.ChatController;
import com.Sunrise.DTO.DBResults.*;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Services.DataServices.DataAccessService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ChatService {

    private final LockService lockService;
    private final DataAccessService dataAccessService;

    public ChatService(LockService lockService, DataAccessService dataAccessService) {
        this.lockService = lockService;
        this.dataAccessService = dataAccessService;
    }

    public HistoryOperationResult clearChatHistory(Long chatId, ChatController.ClearType clearType, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if(dataAccessService.notExistsUserById(userId))
                return new HistoryOperationResult(false, "User not found", null);

            if(!dataAccessService.existsChat(chatId))
                return new HistoryOperationResult(false, "Chat not found", null);

            if(!dataAccessService.isUserInChat(userId, chatId))
                return new HistoryOperationResult(false, "User is not a member of this chat", null);

            var messages_count = switch (clearType) {
                case FOR_ALL -> dataAccessService.clearChatHistoryForAll(chatId, userId); // Уведомить всех надо об этом
                case FOR_SELF -> dataAccessService.clearChatHistoryForSelf(chatId, userId);  // Уведомить пользователя надо об этом
            };

            return new HistoryOperationResult(true, null, messages_count);
        }
        catch (Exception e) {
            return new HistoryOperationResult(false, "ClearChatHistory failed due to server error", 0);
        }
        finally
        {
            lockService.unlockWriteChat(chatId);
            lockService.unlockReadUser(userId);
        }
    } // Уведомить пользователя надо об этом
    public HistoryOperationResult restoreChatHistory(Long chatId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if(dataAccessService.notExistsUserById(userId))
                return new HistoryOperationResult(false, "User not found", null);

            if(!dataAccessService.existsChat(chatId))
                return new HistoryOperationResult(false, "Chat not found", null);

            if(dataAccessService.isUserInChat(userId, chatId))
                return new HistoryOperationResult(false, "User is not a member of this chat", null);

            var messages_count = dataAccessService.restoreChatHistoryForSelf(chatId, userId); // Уведомить пользователя надо об этом

            return new HistoryOperationResult(true, null, messages_count);
        }
        catch (Exception e) {
            return new HistoryOperationResult(false, "RestoreChatHistory failed due to server error", null);
        }
        finally
        {
            lockService.unlockWriteChat(chatId);
            lockService.unlockReadUser(userId);
        }
    } // Уведомить пользователя надо об этом

    public ChatCreationOperationResult createPersonalChat(Long userId, Long otherUserId) {

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockWriteUser(otherUserId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        lockService.lockWriteUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ
        try
        {
            if (userId.equals(otherUserId))
                return new ChatCreationOperationResult(false, "Cannot create personal chat with yourself", null);

            if (dataAccessService.notExistsUserById(userId) || dataAccessService.notExistsUserById(otherUserId))
                return new ChatCreationOperationResult(false, "One or both users not found", null);

            Long chatId = dataAccessService.makePersonalChatAndAddPeople(userId, otherUserId);

            return new ChatCreationOperationResult(true, null, chatId);
        }
        catch (Exception e)
        {
            return new ChatCreationOperationResult(false, "CreatePersonalChat failed due to server error", (long)0);
        }
        finally
        {
            lockService.unlockGlobalChats();
            lockService.unlockWriteUser(otherUserId);
            lockService.unlockWriteUser(userId);
        }
    }
    public ChatCreationOperationResult createGroupChat(String chatName, Long createdBy, Set<Long> usersId) {

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        usersId.forEach(lockService::lockWriteUser); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫХ ПОЛЬЗОВАТЕЛЕЙ
        lockService.lockWriteUser(createdBy); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ
        try
        {
            if (chatName == null || chatName.trim().length() < 4)
                return new ChatCreationOperationResult(false, "Group chat name must be at least 4 characters", null);

            if (dataAccessService.notExistsUserById(createdBy))
                return new ChatCreationOperationResult(false, "Creator not found", null);

            if (usersId.contains(createdBy))
                return new ChatCreationOperationResult(false, "Creator cannot be in member list", null);

            for (Long userId : usersId)
            {
                if (dataAccessService.notExistsUserById(userId))
                    return new ChatCreationOperationResult(false, "User not found: " + userId, null);
            }

            // Создание чата
            Long chatId = dataAccessService.makeGroupChatAndAddPeople(chatName.trim(), createdBy, usersId);

            return new ChatCreationOperationResult(true, null, chatId);
        }
        catch (Exception e)
        {
            return new ChatCreationOperationResult(false, "CreateGroupChat failed due to server error", (long)0);
        }
        finally
        {
            lockService.unlockGlobalChats();
            usersId.forEach(lockService::unlockWriteUser);
            lockService.unlockWriteUser(createdBy);
        }
    }

    public SimpleResult addGroupMember(Long chatId, Long inviterId, Long newUserId) {

        lockService.lockWriteChat(chatId);  // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ
        lockService.lockWriteUser(newUserId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ
        lockService.lockReadUser(inviterId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ) - ПРИГЛАСИТЕЛЯ НЕ БЛОКИРУЕМ НА ЗАПИСЬ
        try
        {
            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
                return new SimpleResult(false, "Chat not found");

            if (!isGroup.get())
                return new SimpleResult(false, "Cannot add members to personal chat");

//            if (dataAccessService.notExistsUserById(inviterId))
//                return new SimpleResult(false, "Inviter not found");

            var isChatAdmin = dataAccessService.isChatAdmin(chatId, inviterId);

            if (isChatAdmin.isEmpty())
                return new SimpleResult(false, "Cannot find user");

            if (!isChatAdmin.get())
                return new SimpleResult(false, "Only admin can add members to group");

            if (dataAccessService.notExistsUserById(newUserId))
                return new SimpleResult(false, "User not found");

            if (!dataAccessService.isUserInChat(chatId, newUserId))
                return new SimpleResult(false, "User is already a member of this group");

            dataAccessService.addUserToChat(chatId, newUserId, false); // Надо всех уведомить

            return new SimpleResult(true, null);
        }
        catch (Exception e) {
            return new SimpleResult(false, "AddGroupMember failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
            lockService.unlockWriteUser(newUserId);
            lockService.unlockReadUser(inviterId);
        }
    } // Надо всех уведомить
    public SimpleResult leaveChat(Long chatId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ
        lockService.lockWriteUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return new SimpleResult(false, "User not found");

            if (!dataAccessService.isUserInChat(chatId, userId))
                return new SimpleResult(false, "User is not a member of this chat");

            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
                return new SimpleResult(false, "Chat not found");

            if (isGroup.get())
            {
                var creatorId = dataAccessService.getChatCreator(chatId);

                if (creatorId.isEmpty())
                    return new SimpleResult(false, "Chat not found");

                leaveGroupChatHandler(chatId, userId, creatorId.get());
            }
            else dataAccessService.deleteChat(chatId); // Личный чат - удаляем полностью

            return new SimpleResult(true, null);
        }
        catch (Exception e) {
            return new SimpleResult(false, "LeaveChat failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
            lockService.unlockWriteUser(userId);
        }
    } // Надо всех уведомить                TODO: ПОДУМАТЬ И СПРОСИТЬ У ПАПЫ
    private void leaveGroupChatHandler(Long chatId, Long userId, Long creatorId){
        if (userId.equals(creatorId)) {

            var anotherAdmin = dataAccessService.findAnotherAdmin(chatId, userId);

            if (anotherAdmin.isPresent()) // Ищем другого админа для передачи прав
            {
                dataAccessService.updateChatCreator(chatId, anotherAdmin.get());
                dataAccessService.removeUserFromChat(chatId, userId);
            }
            else dataAccessService.deleteChat(chatId); // Удаляем чат, если больше пользователей нет
        }
        else dataAccessService.removeUserFromChat(chatId, userId); // Не создатель - выходим
    }

    public ChatStatsOperationResult getChatStats(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ (ЧТЕНИЕ)
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if(dataAccessService.notExistsUserById(userId))
                return new ChatStatsOperationResult(false, "User not found", null, null, null, null);

            if(!dataAccessService.existsChat(chatId))
                return new ChatStatsOperationResult(false, "Chat not found", null, null, null, null);

            if(!dataAccessService.isUserInChat(chatId, userId))
                return new ChatStatsOperationResult(false, "User is not a member of this chat", null, null, null, null);

            var result = dataAccessService.getChatClearStats(chatId, userId);

            return new ChatStatsOperationResult(true, null, result.getTotalMessages(), result.getDeletedForAll(), result.getHiddenByUser(), result.getCanClearForAll());
        }
        catch (Exception e) {
            return new ChatStatsOperationResult(false, "GetChatStats failed due to server error", 0, 0, 0, false);
        }
        finally {
            lockService.unlockReadChat(chatId);
            lockService.unlockReadUser(userId);
        }
    }
    public GetChatMessagesResult getChatMessages(Long chatId, Long userId, Integer limit, Integer offset) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ (ЧТЕНИЕ)
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return new GetChatMessagesResult(false, "User not found", null);

            if (!dataAccessService.existsChat(chatId))
                return new GetChatMessagesResult(false, "Chat not found", null);

            if (!dataAccessService.isUserInChat(chatId, userId))
                return new GetChatMessagesResult(false, "User is not a member of this chat", null);

            List<MessageResult> messages = dataAccessService.getChatMessages(chatId, userId, limit, offset);

            return new GetChatMessagesResult(true, null, messages);
        }
        catch (Exception e) {
            return new GetChatMessagesResult(false, "GetChatMessages failed due to server error", null);
        }
        finally {
            lockService.unlockReadChat(chatId);
            lockService.unlockReadUser(userId);
        }
    }

    public IsChatAdminResult isChatAdmin(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ (ЧТЕНИЕ)
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return new IsChatAdminResult(false, "User not found", null);

            if (!dataAccessService.existsChat(chatId))
                return new IsChatAdminResult(false, "Chat not found", null);

            if (!dataAccessService.isUserInChat(chatId, userId))
                return new IsChatAdminResult(false, "User is not a member of this chat", null);

            var isChatAdmin = dataAccessService.isChatAdmin(chatId, userId);

            if (isChatAdmin.isEmpty())
                return new IsChatAdminResult(false, "Cannot find user", null);

            return new IsChatAdminResult(true, null, isChatAdmin.get());
        }
        catch (Exception e) {
            return new IsChatAdminResult(false, "GetIsChatAdmin failed due to server error", false);
        }
        finally {
            lockService.unlockReadChat(chatId);
            lockService.unlockReadUser(userId);
        }
    }
    public IsGroupChatResult isGroupChat(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ (ЧТЕНИЕ)
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return new IsGroupChatResult(false, "User not found", null);

            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
            {
                return new IsGroupChatResult(false, "Chat not found", null);
            }
            else return new IsGroupChatResult(true, null, isGroup.get());
        }
        catch (Exception e) {
            return new IsGroupChatResult(false, "GetIsGroupChat failed due to server error", null);
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }

    public SimpleResult markMessageAsRead(Long chatId, Long messageId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return new SimpleResult(false, "User not found");

            if (!dataAccessService.existsChat(chatId))
                return new SimpleResult(false, "Chat not found");

            if (!dataAccessService.isUserInChat(chatId, userId))
                return new SimpleResult(false, "User is not a member of this chat");

            dataAccessService.markMessageAsRead(messageId, userId); // уведомить всех надо об этом

            return new SimpleResult(true, null);
        }
        catch (Exception e) {
            return new SimpleResult(false, "MarkMessageAsRead failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
            lockService.unlockReadUser(userId);
        }
    } // Надо всех уведомить
    public VisibleMessagesCountResult getVisibleMessagesCount(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ОПРЕДЕЛЕННЫЙ ЧАТ (ЧТЕНИЕ)
        lockService.lockReadUser(userId); // БЛОКИРУЕМ ОПРЕДЕЛЕННОГО ПОЛЬЗОВАТЕЛЯ (ЧТЕНИЕ)
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return new VisibleMessagesCountResult(false, "User not found", null);

            if (!dataAccessService.existsChat(chatId))
                return new VisibleMessagesCountResult(false, "Chat not found", null);

            if (!dataAccessService.isUserInChat(chatId, userId))
                return new VisibleMessagesCountResult(false, "User is not a member of this chat", null);

            int count = dataAccessService.getVisibleMessagesCount(chatId, userId);

            return new VisibleMessagesCountResult(true, null, count);
        }
        catch (Exception e) {
            return new VisibleMessagesCountResult(false, "GetVisibleMessagesCount failed due to server error", null);
        }
        finally{
            lockService.unlockReadChat(chatId);
            lockService.unlockReadUser(userId);
        }
    }
}