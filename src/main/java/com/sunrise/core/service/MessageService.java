package com.sunrise.core.service;

import com.sunrise.core.service.result.ChatMessagesResult;
import com.sunrise.core.service.result.CreateMessageResult;
import com.sunrise.core.service.result.SimpleResult;
import com.sunrise.entity.dto.MessagesPageDTO;
import com.sunrise.entity.dto.LightMessageDTO;
import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.core.dataservice.DataValidator;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.Set;

@Slf4j
@Service
public class MessageService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;

    public MessageService(DataValidator validator, DataOrchestrator dataOrchestrator){
        this.validator = validator;
        this.dataOrchestrator = dataOrchestrator;
    }

    public CreateMessageResult makePublicMessage(long chatId, long userId, String text) {
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
    }
    public CreateMessageResult makePrivateMessage(long chatId, long senderId, long userToSend, String text) {
        try {
            validator.validateCanSendPrivateMessage(chatId, senderId, userToSend);

            // Валидация текста сообщения
            if (text == null || text.trim().isEmpty())
                return CreateMessageResult.error("Message text cannot be empty");

            if (text.length() > 10000)
                return CreateMessageResult.error("Message text is too long");

            var message = LightMessageDTO.create(SimpleSnowflakeId.nextId(), chatId, senderId, text);

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
    }

    public ChatMessagesResult getChatMessages(long chatId, long userId, Long cursor, int limit, Direction direction) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            MessagesPageDTO pagination = dataOrchestrator.getChatMessagesPage(chatId, userId, cursor, limit, direction);

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
    }

    public SimpleResult markMessageAsRead(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);
            validator.validateActiveMessage(chatId, userId);

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
    }
}