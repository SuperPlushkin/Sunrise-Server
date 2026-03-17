package com.Sunrise.Services;

import com.Sunrise.DTO.ServiceResults.*;
import com.Sunrise.Entities.DB.Message;
import com.Sunrise.Entities.DTO.LightMessageDTO;
import com.Sunrise.Services.DataServices.DataAccessService;
import com.Sunrise.Services.DataServices.DataValidator;
import com.Sunrise.Services.DataServices.LockService;
import com.Sunrise.Subclasses.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.Sunrise.Services.DataServices.DataAccessService.randomId;

@Slf4j
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

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return CreateMessageResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            Message message = Message.create(randomId(), userId, chatId, text);

            dataAccessService.saveMessage(message);

            // Отправляем сообщение в очередь на отправку всем участникам

            return CreateMessageResult.success(message.getId(), message.getSentAt());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed making public message: {}", e.getMessage());
            return CreateMessageResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error making public message: {}", e.getMessage());
            return CreateMessageResult.error("createPublicMessage failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
    public CreateMessageResult makePrivateMessage(Long chatId, Long userId, Long userToSend, String text) {

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return CreateMessageResult.error("Try again later");

        try {
            validator.validateActiveUsersInActiveChatAndChatIsPersonal(chatId, Set.of(userId, userToSend));

            // Валидация текста сообщения
            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            Message message = Message.create(randomId(), userId, chatId, text);

            // Отправляем приватное сообщение (при этом оно не хранится на сервере, надо удостовериться, что отослалось. Если нет - то ошибка)

            return CreateMessageResult.success(message.getId(), message.getSentAt());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed making private message: {}", e.getMessage());
            return CreateMessageResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error making private message: {}", e.getMessage());
            return CreateMessageResult.error("createPrivateMessage failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }

    public ChatMessagesResult getChatMessagesUpToDate(Long chatId, Long userId, Integer limit) {

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return ChatMessagesResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            List<LightMessageDTO> messages = dataAccessService.getChatMessagesUpToDate(chatId, userId, limit);

            return ChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting messages up-to-date: {}", e.getMessage());
            return ChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting messages up-to-date: {}", e.getMessage());
            return ChatMessagesResult.error("getChatMessagesUpToDate failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
    public ChatMessagesResult getChatMessagesBefore(Long chatId, Long userId, Long messageId, Integer limit) {

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return ChatMessagesResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            List<LightMessageDTO> messages = dataAccessService.getChatMessagesBefore(chatId, userId, messageId, limit);

            return ChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting messages before: {}", e.getMessage());
            return ChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting messages before: {}", e.getMessage());
            return ChatMessagesResult.error("getChatMessagesBefore failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
    public ChatMessagesResult getChatMessagesAfter(Long chatId, Long userId, Long messageId, Integer limit) {

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return ChatMessagesResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            List<LightMessageDTO> messages = dataAccessService.getChatMessagesAfter(chatId, userId, messageId, limit);

            return ChatMessagesResult.success(messages);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting messages after: {}", e.getMessage());
            return ChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting messages after: {}", e.getMessage());
            return ChatMessagesResult.error("getChatMessagesAfter failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }

    public VisibleMessagesCountResult getVisibleMessagesCount(Long chatId, Long userId) {

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return VisibleMessagesCountResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            int count = dataAccessService.getVisibleMessagesCount(chatId, userId);

            return new VisibleMessagesCountResult(true, null, count);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting visible messages count: {}", e.getMessage());
            return VisibleMessagesCountResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting visible messages count: {}", e.getMessage());
            return VisibleMessagesCountResult.error("GetVisibleMessagesCount failed due to server error");
        }
        finally{
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
    public SimpleResult markMessageAsRead(Long chatId, Long messageId, Long userId) {

        // READ на чат + READ на пользователя
        if (!lockService.tryLockChatReadUserRead(chatId, userId))
            return SimpleResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            dataAccessService.markMessageAsRead(messageId, userId); // уведомить всех надо об этом

            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed marking message as read: {}", e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error marking message as read: {}", e.getMessage());
            return SimpleResult.error("MarkMessageAsRead failed due to server error");
        }
        finally {
            lockService.unLockChatReadUserRead(chatId, userId);
        }
    }
}