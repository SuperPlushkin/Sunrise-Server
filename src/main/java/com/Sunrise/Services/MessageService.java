package com.Sunrise.Services;

import com.Sunrise.DTO.DBResults.MessageResult;
import com.Sunrise.DTO.ServiceResults.CreateMessageResult;
import com.Sunrise.DTO.ServiceResults.GetChatMessagesResult;
import com.Sunrise.DTO.ServiceResults.SimpleResult;
import com.Sunrise.DTO.ServiceResults.VisibleMessagesCountResult;
import com.Sunrise.Entities.Message;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Subclasses.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.Sunrise.Services.DataServices.DataAccessService.generateRandomId;

@Service
public class MessageService {

    private final DataValidator validator;
    private final DataAccessService dataAccessService;
    private final LockService lockService;

    public MessageService(DataValidator validator, DataAccessService dataAccessService, LockService lockService){
        this.validator = validator;
        this.dataAccessService = dataAccessService;
        this.lockService = lockService;
    }

    public CreateMessageResult makePublicMessage(Long chatId, Long userId, String text){

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ (ЗАПИСЬ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            Message message = Message.create(generateRandomId(), userId, chatId, text);

            dataAccessService.saveMessage(message);

            // Отправляем сообщение в очередь на отправку всем участникам

            return CreateMessageResult.success(message.getId(), message.getSentAt());
        }
        catch (ValidationException e) {
            return CreateMessageResult.error(e.getMessage());
        }
        catch (Exception e) {
            return CreateMessageResult.error("createPublicMessage failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    }
    public CreateMessageResult makePrivateMessage(Long chatId, Long userId, Long userToSend, String text) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ (ЗАПИСЬ)
        try {
            if (validator.validateUsersInChatAndGetIsGroup(chatId, Set.of(userId, userToSend)))
                return CreateMessageResult.error("Chat is a personal chat");

            // Валидация текста сообщения
            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            Message message = Message.create(generateRandomId(), userId, chatId, text);

            // Отправляем приватное сообщение (при этом оно не хранится на сервере, надо удостовериться, что отослалось. Если нет - то ошибка)

            return CreateMessageResult.success(message.getId(), message.getSentAt());
        }
        catch (ValidationException e) {
            return CreateMessageResult.error(e.getMessage());
        }
        catch (Exception e) {
            return CreateMessageResult.error("createPrivateMessage failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    }

    public GetChatMessagesResult getChatMessagesFirst(Long chatId, Long userId, Integer limit) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            List<MessageResult> messages = dataAccessService.getChatMessagesFirst(chatId, userId, limit);

            return GetChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            return GetChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            return GetChatMessagesResult.error("getChatMessagesFirst failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
    public GetChatMessagesResult getChatMessagesBefore(Long chatId, Long userId, Long messageId, Integer limit) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            List<MessageResult> messages = dataAccessService.getChatMessagesBefore(chatId, userId, messageId, limit);

            return GetChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            return GetChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            return GetChatMessagesResult.error("getChatMessagesBefore failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }
    public GetChatMessagesResult getChatMessagesAfter(Long chatId, Long userId, Long messageId, Integer limit) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            List<MessageResult> messages = dataAccessService.getChatMessagesAfter(chatId, userId, messageId, limit);

            return GetChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            return GetChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            return GetChatMessagesResult.error("getChatMessagesAfter failed due to server error");
        }
        finally {
            lockService.unlockReadChat(chatId);
        }
    }

    public VisibleMessagesCountResult getVisibleMessagesCount(Long chatId, Long userId) {

        lockService.lockReadChat(chatId); // БЛОКИРУЕМ ЧАТ (ЧТЕНИЕ)
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            int count = dataAccessService.getVisibleMessagesCount(chatId, userId);

            return new VisibleMessagesCountResult(true, null, count);
        }
        catch (ValidationException e) {
            return VisibleMessagesCountResult.error(e.getMessage());
        }
        catch (Exception e) {
            return VisibleMessagesCountResult.error("GetVisibleMessagesCount failed due to server error");
        }
        finally{
            lockService.unlockReadChat(chatId);
        }
    }
    public SimpleResult markMessageAsRead(Long chatId, Long messageId, Long userId) {

        lockService.lockWriteChat(chatId); // БЛОКИРУЕМ ЧАТ
        try
        {
            validator.validateActiveUserInChat(chatId, userId);

            dataAccessService.markMessageAsRead(messageId, userId); // уведомить всех надо об этом

            return SimpleResult.success();
        }
        catch (ValidationException e) {
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            return SimpleResult.error("MarkMessageAsRead failed due to server error");
        }
        finally {
            lockService.unlockWriteChat(chatId);
        }
    }
}
