package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentWsUserId;
import com.sunrise.core.service.MessageService;
import com.sunrise.entity.wsdto.WebSocketMessageDTO;

import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;


@Slf4j
@Controller
public class WSMessageController {
    private final MessageService messageService;

    public WSMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @MessageMapping("/chat/{chatId}/send")
    public void sendPublicMessage(@DestinationVariable long chatId, @Payload WebSocketMessageDTO message, @CurrentWsUserId long userId) {
        messageService.makePublicMessage(chatId, userId, message.getText());
    }

    @MessageMapping("/chat/{chatId}/read")
    public void markAsRead(@DestinationVariable long chatId, @Payload long messageId, @CurrentWsUserId long userId) {
        messageService.markMessagesUpToRead(chatId, userId, messageId);
    }
}