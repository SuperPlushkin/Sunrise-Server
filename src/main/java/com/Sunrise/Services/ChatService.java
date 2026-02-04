package com.Sunrise.Services;

import com.Sunrise.Controllers.ChatController;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Entities.Chat;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Services.DataServices.CacheEntities.FullChatMember;
import com.Sunrise.Subclasses.ValidationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.Sunrise.Services.DataServices.DataAccessService.generateRandomId;

@Service
public class ChatService {

    private final LockService lockService;
    private final DataAccessService dataAccessService;
    private final DataValidator validator;

    public ChatService(LockService lockService, DataAccessService dataAccessService, DataValidator validator) {
        this.lockService = lockService;
        this.dataAccessService = dataAccessService;
        this.validator = validator;
    }

    public ChatCreationOperationResult createPersonalChat(Long creatorId, Long userToAddId) {

        if (creatorId.equals(userToAddId))
            return ChatCreationOperationResult.error("Cannot create personal chat with yourself");

        Set<Long> usersId = Set.of(creatorId, userToAddId);

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockUsersSafely(usersId, true); // БЛОКИРУЕМ ПОЛЬЗОВАТЕЛЕЙ
        try
        {
            validator.validateActiveUser(creatorId);
            validator.validateActiveUser(userToAddId);

            if (dataAccessService.findPersonalChat(creatorId, userToAddId) instanceof Optional<Long> chatId && chatId.isPresent())
                return ChatCreationOperationResult.success(chatId.get());

            if (dataAccessService.findDeletedPersonalChat(creatorId, userToAddId) instanceof Optional<Long> chatId && chatId.isPresent()){
                dataAccessService.restoreChat(chatId.get());
                return ChatCreationOperationResult.success(chatId.get());
            }

            Chat chat = Chat.createPersonalChat(generateRandomId(), creatorId);

            dataAccessService.savePersonalChatAndAddPerson(chat, userToAddId);

            return ChatCreationOperationResult.success(chat.getId());
        }
        catch (ValidationException e) {
            return ChatCreationOperationResult.error(e.getMessage());
        }
        catch (Exception e) {
            return ChatCreationOperationResult.error("CreatePersonalChat failed due to server error");
        }
        finally {
            lockService.unlockUsersSafely(usersId, true);
            lockService.unlockGlobalChats();
        }
    }
    public ChatCreationOperationResult createGroupChat(String chatName, Long creatorId, Set<Long> usersToAddId) {

        if (usersToAddId.contains(creatorId))
            return ChatCreationOperationResult.error("Creator in usersToAdd");

        Set<Long> allUserIds = new HashSet<>(usersToAddId);
        allUserIds.add(creatorId);

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockUsersSafely(allUserIds, false); // БЛОКИРУЕМ ПОЛЬЗОВАТЕЛЕЙ
        try
        {
            validator.validateActiveUser(creatorId);

            for (Long userToAddId : usersToAddId) {
                validator.validateActiveUser(userToAddId);
            }

            Chat chat = Chat.createGroupChat(generateRandomId(), chatName, creatorId);

            dataAccessService.saveGroupChatAndAddPeople(chat, usersToAddId);

            return ChatCreationOperationResult.success(chat.getId());
        }
        catch (ValidationException e) {
            return ChatCreationOperationResult.error(e.getMessage());
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
            validator.validateAddGroupMember(chatId, inviterId, newUserId);

            dataAccessService.addUserToChat(chatId, newUserId, false); // Надо всех уведомить

            return SimpleResult.success();
        }
        catch (ValidationException e) {
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            return SimpleResult.error("AddGroupMember failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    }
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
                        dataAccessService.updateChatCreator(chatId, anotherAdmin.get()); // Передаем права создателя другому админу
                        dataAccessService.removeUserFromChat(chatId, userId); // Выходим из чата
                    }
                    else dataAccessService.deleteChat(chatId); // Удаляем чат, если больше пользователей нет
                }
                else dataAccessService.removeUserFromChat(chatId, userId); // Просто выходим из чата
            }
            else dataAccessService.deleteChat(chatId); // Личный чат - удаляем полностью

            return SimpleResult.success();
        }
        catch (ValidationException e) {
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            return SimpleResult.error("LeaveChat failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    }

    public HistoryOperationResult clearChatHistory(Long chatId, ChatController.ClearType clearType, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

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
    }

    public GetUserChatsResult getUserChats(Long userId) {

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        try
        {
            validator.validateActiveUser(userId);

            var result = dataAccessService.getUserChats(userId);

            if(result.isEmpty())
                return GetUserChatsResult.error("Cannot find users chats");

            List<Chat> chats = result.get();

            return GetUserChatsResult.success(chats, chats.size());
        }
        catch (ValidationException e) {
            return GetUserChatsResult.error(e.getMessage());
        }
        catch (Exception e) {
            return GetUserChatsResult.error("getUserChats failed due to server error");
        }
        finally {
            lockService.unlockGlobalChats();
        }
    }
    public ChatStatsOperationResult getChatStats(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

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
    public GetChatMembersResult getChatMembers(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            var chatMembersOptional = dataAccessService.getChatMembers(chatId);

            if(chatMembersOptional.isEmpty())
                return GetChatMembersResult.error("Cannot find chat members");

            Set<FullChatMember> chatMembers = chatMembersOptional.get();

            return GetChatMembersResult.success(chatMembers, chatMembers.size());
        }
        catch (ValidationException e) {
            return GetChatMembersResult.error(e.getMessage());
        }
        catch (Exception e) {
            return GetChatMembersResult.error("getUserChats failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
}