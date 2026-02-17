package com.Sunrise.Services;

import com.Sunrise.Controllers.ChatController;
import com.Sunrise.DTO.Responses.ChatMemberDTO;
import com.Sunrise.DTO.Responses.ChatDTO;
import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Entities.DB.Chat;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Entities.DB.User;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Services.DataServices.DataValidator;
import com.Sunrise.Services.DataServices.LockService;
import com.Sunrise.Subclasses.ValidationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.Sunrise.Services.DataServices.DataAccessService.generateRandomId;

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

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockUsersSafely(usersId, true); // БЛОКИРУЕМ ПОЛЬЗОВАТЕЛЕЙ
        try
        {
            validator.validateActiveUser(creatorId);
            validator.validateActiveUser(userToAddId);

            if (dataAccessService.getPersonalChatId(creatorId, userToAddId) instanceof Optional<Long> chatId && chatId.isPresent())
                return ChatCreationResult.success(chatId.get());

            if (dataAccessService.getDeletedPersonalChatId(creatorId, userToAddId) instanceof Optional<Long> chatId && chatId.isPresent()){
                dataAccessService.restoreChat(chatId.get());
                return ChatCreationResult.success(chatId.get());
            }

            Chat chat = Chat.createPersonalChat(generateRandomId(), creatorId);

            var creator = new ChatMember(chat.getId(), creatorId, true);
            var member = new ChatMember(chat.getId(), userToAddId, false);

            dataAccessService.savePersonalChatAndAddPerson(chat, creator, member);

            return ChatCreationResult.success(chat.getId());
        }
        catch (ValidationException e) {
            return ChatCreationResult.error(e.getMessage());
        }
        catch (Exception e) {
            return ChatCreationResult.error("CreatePersonalChat failed due to server error");
        }
        finally {
            lockService.unlockUsersSafely(usersId, true);
            lockService.unlockGlobalChats();
        }
    } // TODO: СЛИШКОМ НАМУДРИЛ, НАДО ПРОЩЕ
    public ChatCreationResult createGroupChat(String chatName, Long creatorId, Set<Long> usersToAddId) {

        if (usersToAddId.contains(creatorId))
            return ChatCreationResult.error("Creator in usersToAdd");

        Set<Long> allUserIds = new HashSet<>(usersToAddId);
        allUserIds.add(creatorId);

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        lockService.lockUsersSafely(allUserIds, false); // БЛОКИРУЕМ ПОЛЬЗОВАТЕЛЕЙ
        try
        {
            validator.validateActiveUser(creatorId);

            Chat chat = Chat.createGroupChat(generateRandomId(), chatName, creatorId);

            List<ChatMember> members = new ArrayList<>(usersToAddId.size() + 1);
            members.add(new ChatMember(chat.getId(), creatorId, true));

            for (Long userToAddId : usersToAddId) {
                validator.validateActiveUser(userToAddId);
                members.add(new ChatMember(chat.getId(), userToAddId, false));
            }

            dataAccessService.saveGroupChatAndAddPeople(chat, members);

            return ChatCreationResult.success(chat.getId());
        }
        catch (ValidationException e) {
            return ChatCreationResult.error(e.getMessage());
        }
        catch (Exception e) {
            return ChatCreationResult.error("CreateGroupChat failed due to server error");
        }
        finally {
            lockService.unlockUsersSafely(allUserIds, false);
            lockService.unlockGlobalChats();
        }
    } // TODO: ПЕРЕДЕЛАТЬ LOCKs

    public SimpleResult addGroupMember(Long chatId, Long inviterId, Long userToAddId) {

        if(inviterId.equals(userToAddId))
            return SimpleResult.error("Tы даун?");

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            validator.validateActiveUser(inviterId);
            validator.validateActiveUser(userToAddId);

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

            if (dataAccessService.hasChatMember(chatId, userToAddId))
                throw new ValidationException("User is already a member of this group");

            dataAccessService.saveChatMember(new ChatMember(chatId, userToAddId, false)); // Надо всех уведомить

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
            if (!dataAccessService.existsUser(userId))
                throw new ValidationException("User not found");

            if (!dataAccessService.hasChatMember(chatId, userId))
                throw new ValidationException("User is not a member of this chat");

            Optional<Boolean> isGroup = dataAccessService.isGroupChat(chatId);

            if (isGroup.isEmpty())
                throw new ValidationException("Chat not found");

            if (isGroup.get())
            {
                var creatorId = dataAccessService.getChatCreator(chatId);

                if (creatorId.isEmpty())
                    throw new ValidationException("Chat not found");

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

    public UserChatsResult getUserChats(Long userId) {

        lockService.lockGlobalChats(); // БЛОКИРУЕМ СПИСОК ЧАТОВ
        try
        {
            validator.validateActiveUser(userId);

            var result = dataAccessService.getUserChats(userId);

            if(result.isEmpty())
                return UserChatsResult.error("Cannot find users chats");

            Set<ChatDTO> chats = result.get().stream().map(ChatDTO::fromCacheChat).collect(Collectors.toSet());

            return UserChatsResult.success(chats, chats.size());
        }
        catch (ValidationException e) {
            return UserChatsResult.error(e.getMessage());
        }
        catch (Exception e) {
            return UserChatsResult.error("getUserChats failed due to server error");
        }
        finally {
            lockService.unlockGlobalChats();
        }
    }
    public ChatStatsResult getChatStats(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            var result = dataAccessService.getChatClearStats(chatId, userId);

            return ChatStatsResult.success(result.getTotalMessages(), result.getDeletedForAll(), result.getHiddenByUser(), result.getCanClearForAll());
        }
        catch (ValidationException e) {
            return ChatStatsResult.error(e.getMessage());
        }
        catch (Exception e) {
            return ChatStatsResult.error("GetChatStats failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
    public ChatMembersResult getChatMembers(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            var chatMembersOptional = dataAccessService.getChatMembers(chatId);

            if(chatMembersOptional.isEmpty())
                return ChatMembersResult.error("Cannot find chat members");

            var chatMembers = chatMembersOptional.get().stream()
                                    .map(ChatMemberDTO::fromCacheChatMember).collect(Collectors.toSet());

            return ChatMembersResult.success(chatMembers, chatMembers.size());
        }
        catch (ValidationException e) {
            return ChatMembersResult.error(e.getMessage());
        }
        catch (Exception e) {
            return ChatMembersResult.error("getUserChats failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
}