package com.sunrise.core.service;

import com.sunrise.core.service.result.*;
import com.sunrise.entity.dto.MessageReadStatusDTO;
import com.sunrise.entity.dto.MessagesPageDTO;
import com.sunrise.entity.dto.MessageDTO;
import com.sunrise.core.dataservice.DataOrchestrator;
import com.sunrise.core.dataservice.type.Direction;
import com.sunrise.core.dataservice.DataValidator;
import com.sunrise.helpclass.SimpleSnowflakeId;
import com.sunrise.helpclass.ValidationException;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class MessageService {

    private final DataValidator validator;
    private final DataOrchestrator dataOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;

    public CreateMessageResult makePublicMessage(long chatId, long senderId, String text) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, senderId);

            if (text == null || text.trim().isEmpty()) {
                throw new ValidationException("Message text cannot be empty");
            }

            if (text.length() > 10000) {
                throw new ValidationException("Message text is too long");
            }

            var message = MessageDTO.create(SimpleSnowflakeId.nextId(), chatId, senderId, text);

            dataOrchestrator.saveMessage(message);

            // Отправляем сообщение в очередь на отправку всем участникам

            log.info("[🔧] ✅ User {} send public message {} in chat {}", senderId, message.getId(), chatId);
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
            if (text == null || text.trim().isEmpty()) {
                throw new ValidationException("Message text cannot be empty");
            }

            if (text.length() > 10000) {
                throw new ValidationException("Message text is too long");
            }

            var message = MessageDTO.create(SimpleSnowflakeId.nextId(), chatId, senderId, text);

            // Отправляем приватное сообщение (при этом оно не хранится на сервере, надо удостовериться, что отослалось. Если нет - то ошибка)

            log.info("[🔧] ✅ User {} send private message {} to user {} in chat {}", senderId, message.getId(), userToSend, chatId);
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

    public SimpleResult deleteMessage(long chatId, long userId, long messageId) {
        try {
            validator.validateCanDeleteMessage(chatId, userId, messageId);

            dataOrchestrator.deleteMessage(chatId, messageId); // уведомить всех надо об этом

            log.info("[🔧] ✅ User {} deleted message {} in chat {}", userId, messageId, chatId);
            return SimpleResult.success();
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed deleting message: {}", e.getMessage());
            return SimpleResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error deleting message: {}", e.getMessage());
            return SimpleResult.error("deleteMessage failed due to server error");
        }
    }

    public ChatMessagesResult getMessagePagination(long chatId, long userId, Long cursor, int limit, Direction direction) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            MessagesPageDTO pagination = dataOrchestrator.getChatMessagesPage(chatId, userId, cursor, limit, direction);

            log.info("[🔧] ✅ User {} got {} messages in chat {}", userId, pagination.messages().size(), chatId);
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
    public ChatMessageResult getMessage(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);

            Optional<MessageDTO> message = dataOrchestrator.getActiveMessageWithReadStatusInChat(chatId, userId, messageId);
            if (message.isEmpty()) {
                throw new ValidationException("Message not found");
            }

            log.info("[🔧] ✅ User {} got message {} in chat {}", userId, messageId, chatId);
            return ChatMessageResult.success(message.get());
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting message {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return ChatMessageResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting message {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return ChatMessageResult.error("getMessage failed due to server error");
        }
    }
    public MessageReadsResult getMessageReads(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);
            validator.validateActiveMessageInChat(chatId, messageId);

            Map<Long, MessageReadStatusDTO> message = dataOrchestrator.getMessageReads(messageId);

            log.info("[🔧] ✅ User {} got {} reads of {} message in chat {}", userId, message.size(), messageId, chatId);
            return MessageReadsResult.success(message);
        }
        catch (ValidationException e) {
            log.warn("[🔧] ☝️ Failed getting message reads {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return MessageReadsResult.error(e.getMessage());
        }
        catch (Exception e) {
            log.error("[🔧] ⚠️ Error getting message reads {} for user {} in chat {}: {}", userId, messageId, chatId, e.getMessage());
            return MessageReadsResult.error("getMessage failed due to server error");
        }
    }

    public SimpleResult markMessagesUpToRead(long chatId, long userId, long messageId) {
        try {
            validator.validateActiveChatMemberInActiveChat(chatId, userId);
            validator.validateActiveMessageInChat(chatId, messageId);

            dataOrchestrator.markMessagesUpToRead(chatId, userId, messageId, LocalDateTime.now()); // уведомить всех надо об этом

            log.info("[🔧] ✅ User {} marked message as read {} in chat {}", userId, messageId, chatId);
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