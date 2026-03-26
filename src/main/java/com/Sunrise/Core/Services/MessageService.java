package com.Sunrise.Core.Services;

import com.Sunrise.DTOs.Paginations.MessagesPageDTO;
import com.Sunrise.DTOs.ServiceResults.*;
import com.Sunrise.Entities.DTOs.LightMessageDTO;
import com.Sunrise.Core.DataServices.DataOrchestrator;
import com.Sunrise.Core.DataServices.DataOrchestrator.Direction;
import com.Sunrise.Core.DataServices.DataValidator;
import com.Sunrise.Core.DataServices.LockManager;
import com.Sunrise.Subclasses.SimpleSnowflakeId;
import com.Sunrise.Subclasses.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.Set;

@Slf4j
@Service
public class MessageService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final LockManager lockManager;

    public MessageService(DataValidator validator, DataOrchestrator dataOrchestrator, LockManager lockManager){
        this.validator = validator;
        this.dataOrchestrator = dataOrchestrator;
        this.lockManager = lockManager;
    }

    public CreateMessageResult makePublicMessage(long chatId, long userId, String text) {

        // READ на чат + READ на пользователя
        if (!lockManager.tryLockChatReadUserRead(chatId, userId))
            return CreateMessageResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            var message = LightMessageDTO.create(SimpleSnowflakeId.nextId(), chatId, userId, text);

            dataOrchestrator.saveMessage(message);

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
            lockManager.unLockChatReadUserRead(chatId, userId);
        }
    }
    public CreateMessageResult makePrivateMessage(long chatId, long userId, long userToSend, String text) {

        // READ на чат + READ на пользователя
        if (!lockManager.tryLockChatReadUserRead(chatId, userId))
            return CreateMessageResult.error("Try again later");

        try {
            validator.validateActiveUsersInActiveChatAndChatIsPersonal(chatId, Set.of(userId, userToSend));

            // Валидация текста сообщения
            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            var message = LightMessageDTO.create(SimpleSnowflakeId.nextId(), chatId, userId, text);

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
            lockManager.unLockChatReadUserRead(chatId, userId);
        }
    }

    public ChatMessagesResult getChatMessages(long chatId, long userId, Long fromMessageId, int limit, Direction direction) {

        // READ на чат + READ на пользователя
        if (!lockManager.tryLockChatReadUserRead(chatId, userId))
            return ChatMessagesResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            MessagesPageDTO pagination = dataOrchestrator.getChatMessagesPage(chatId, userId, fromMessageId, limit, direction);

            return ChatMessagesResult.success(pagination);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting {} messages {}: {}", limit, direction.name(), e.getMessage());
            return ChatMessagesResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting {} messages {}: {}", limit, direction.name(), e.getMessage());
            return ChatMessagesResult.error("getChatMessagesAfter failed due to server error");
        }
        finally {
            lockManager.unLockChatReadUserRead(chatId, userId);
        }
    }

    public SimpleResult markMessageAsRead(long chatId, long userId, long messageId) {

        // READ на чат + READ на пользователя
        if (!lockManager.tryLockChatReadUserRead(chatId, userId))
            return SimpleResult.error("Try again later");

        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            dataOrchestrator.markMessageAsRead(chatId, userId, messageId, LocalDateTime.now()); // уведомить всех надо об этом

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
            lockManager.unLockChatReadUserRead(chatId, userId);
        }
    }
}