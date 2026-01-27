package com.Sunrise.Services;

import com.Sunrise.Controllers.ChatController;
import com.Sunrise.DTO.DBResults.*;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Entities.Chat;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Subclasses.ValidationException;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.Sunrise.Services.DataServices.DataAccessService.generateRandomId;

@Service
public class ChatService {

    private final LockService lockService;
    private final DataAccessService dataAccessService;

    public ChatService(LockService lockService, DataAccessService dataAccessService) {
        this.lockService = lockService;
        this.dataAccessService = dataAccessService;
    }

    public HistoryOperationResult clearChatHistory(Long chatId, ChatController.ClearType clearType, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            validateUserInChat(chatId, userId);

            var messages_count = switch (clearType) {
                case FOR_ALL -> dataAccessService.clearChatHistoryForAll(chatId, userId); // Уведомить всех надо об этом
                case FOR_SELF -> dataAccessService.clearChatHistoryForSelf(chatId, userId);  // Уведомить пользователя надо об этом
            };

            return HistoryOperationResult.success(messages_count);
        }
        catch (ValidationException e) {
            return HistoryOperationResult.error(e.getMessage());
        }
        catch (Exception e) {
            return HistoryOperationResult.error("ClearChatHistory failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    } // Уведомить пользователя надо об этом
    public HistoryOperationResult restoreChatHistory(Long chatId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            validateUserInChat(chatId, userId);

            var messages_count = dataAccessService.restoreChatHistoryForSelf(chatId, userId); // Уведомить пользователя надо об этом

            return HistoryOperationResult.success(messages_count);
        }
        catch (ValidationException e) {
            return HistoryOperationResult.error(e.getMessage());
        }
        catch (Exception e) {
            return HistoryOperationResult.error("RestoreChatHistory failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    } // Уведомить пользователя надо об этом

    public ChatCreationOperationResult createPersonalChat(Long creatorId, Long userToAddId) {

        if (creatorId.equals(userToAddId))
            return ChatCreationOperationResult.error("Cannot create personal chat with yourself");

        Set<Long> usersId = Set.of(creatorId, userToAddId);

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockUsersSafely(usersId, true); // БЛОКИРУЕМ ПОЛЬЗОВАТЕЛЕЙ
        try
        {
            if (dataAccessService.notExistsUserById(creatorId) || dataAccessService.notExistsUserById(userToAddId))
                return ChatCreationOperationResult.error("One or both users not found");

            if (dataAccessService.findPersonalChat(creatorId, userToAddId) instanceof Optional<Long> chatId && chatId.isPresent())
                return ChatCreationOperationResult.success(chatId.get());

            Chat chat = Chat.createPersonalChat(generateRandomId(), creatorId);

            dataAccessService.savePersonalChatAndAddPerson(chat, userToAddId);

            return ChatCreationOperationResult.success(chat.getId());
        }
        catch (Exception e) {
            return ChatCreationOperationResult.error("CreatePersonalChat failed due to server error");
        }
        finally {
            lockService.unlockUsersSafely(usersId, true);
            lockService.unlockGlobalChats();
        }
    }
    public ChatCreationOperationResult createGroupChat(String chatName, Long createdBy, Set<Long> usersToAddId) {

        if (usersToAddId.contains(createdBy))
            return ChatCreationOperationResult.error("Creator in usersToAdd");

        Set<Long> allUserIds = new HashSet<>(usersToAddId);
        allUserIds.add(createdBy);

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockUsersSafely(allUserIds, false); // БЛОКИРУЕМ ПОЛЬЗОВАТЕЛЕЙ
        try
        {
            if (dataAccessService.notExistsUserById(createdBy))
                return ChatCreationOperationResult.error("Creator not found");

            for (Long userId : usersToAddId)
            {
                if (dataAccessService.notExistsUserById(userId))
                    return ChatCreationOperationResult.error("User not found: " + userId);
            }

            Chat chat = Chat.createGroupChat(generateRandomId(), chatName, createdBy);

            dataAccessService.saveGroupChatAndAddPeople(chat, usersToAddId);

            return ChatCreationOperationResult.success(chat.getId());
        }
        catch (Exception e) {
            return ChatCreationOperationResult.error("CreateGroupChat failed due to server error");
        }
        finally {
            lockService.unlockUsersSafely(allUserIds, false);
            lockService.unlockGlobalChats();
        }
    } // TODO: ПЕРЕДЕЛАТЬ LOCKs

    public SimpleResult addGroupMember(Long chatId, Long inviterId, Long newUserId) {

        if(inviterId.equals(newUserId))
            return SimpleResult.error("Tы даун?");

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
                return SimpleResult.error("Chat not found");

            if (!isGroup.get())
                return SimpleResult.error("Cannot add members to personal chat");

            var isChatAdmin = dataAccessService.isChatAdmin(chatId, inviterId);

            if (isChatAdmin.isEmpty())
                return SimpleResult.error("User not found");

            if (!isChatAdmin.get())
                return SimpleResult.error("Only admin can add members to group");

            if (dataAccessService.notExistsUserById(newUserId))
                return SimpleResult.error("User not found");

            if (dataAccessService.isUserInChat(chatId, newUserId))
                return SimpleResult.error("User is already a member of this group");

            dataAccessService.addUserToChat(chatId, newUserId, false); // Надо всех уведомить

            return SimpleResult.success();
        }
        catch (Exception e) {
            return SimpleResult.error("AddGroupMember failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    } // Надо всех уведомить
    public SimpleResult leaveChat(Long chatId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return SimpleResult.error("User not found");

            if (!dataAccessService.isUserInChat(chatId, userId))
                return SimpleResult.error("User is not a member of this chat");

            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
                return SimpleResult.error("Chat not found");

            if (isGroup.get())
            {
                var creatorId = dataAccessService.getChatCreator(chatId);

                if (creatorId.isEmpty())
                    return SimpleResult.error("Chat not found");

                if (userId.equals(creatorId.get())){
                    var anotherAdmin = dataAccessService.findAnotherAdmin(chatId, userId);

                    if (anotherAdmin.isPresent()) // Ищем другого админа для передачи прав
                    {
                        dataAccessService.updateChatCreator(chatId, anotherAdmin.get());
                        dataAccessService.removeUserFromChat(chatId, userId);
                    }
                    else dataAccessService.deleteChat(chatId); // Удаляем чат, если больше пользователей нет
                }
                else dataAccessService.removeUserFromChat(chatId, userId); // Личный чат - удаляем полностью
            }
            else dataAccessService.deleteChat(chatId); // Личный чат - удаляем полностью

            return SimpleResult.success();
        }
        catch (Exception e) {
            return SimpleResult.error("LeaveChat failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    } // Надо всех уведомить  TODO: ПОДУМАТЬ НАД ПЕРЕДАЧЕЙ АДМИНА

    public ChatStatsOperationResult getChatStats(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validateUserInChat(chatId, userId);

            var result = dataAccessService.getChatClearStats(chatId, userId);

            return ChatStatsOperationResult.success(result.getTotalMessages(), result.getDeletedForAll(), result.getHiddenByUser(), result.getCanClearForAll());
        }
        catch (ValidationException e) {
            return ChatStatsOperationResult.error(e.getMessage());
        }
        catch (Exception e) {
            return ChatStatsOperationResult.error("GetChatStats failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }

    public IsChatAdminResult isChatAdmin(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validateUserInChat(chatId, userId);

            var isChatAdmin = dataAccessService.isChatAdmin(chatId, userId);

            if (isChatAdmin.isEmpty())
                return IsChatAdminResult.error("Cannot find user");

            return IsChatAdminResult.success(isChatAdmin.get());
        }
        catch (ValidationException e) {
            return IsChatAdminResult.error(e.getMessage());
        }
        catch (Exception e) {
            return IsChatAdminResult.error("GetIsChatAdmin failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
    public IsGroupChatResult isGroupChat(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            if (dataAccessService.notExistsUserById(userId))
                return IsGroupChatResult.error("User not found");

            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
            {
                return IsGroupChatResult.error("Chat not found");
            }
            else return IsGroupChatResult.success(isGroup.get());
        }
        catch (Exception e) {
            return IsGroupChatResult.error("GetIsGroupChat failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }


    private void validateUserInChat(Long chatId, Long userId) {
        if (!dataAccessService.existsChat(chatId))
            throw new ValidationException("Chat not found");

        if (dataAccessService.notExistsUserById(userId))
            throw new ValidationException("User not found");

        if (!dataAccessService.isUserInChat(chatId, userId))
            throw new ValidationException("User is not a member of this chat");
    }
}