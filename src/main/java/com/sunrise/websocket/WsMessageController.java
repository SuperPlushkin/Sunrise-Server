package com.sunrise.websocket;

import com.sunrise.config.annotation.WsCurrentUserId;
import com.sunrise.core.notifier.WsNotificationService;
import com.sunrise.core.service.ChatService;
import com.sunrise.core.service.MessageService;
import com.sunrise.core.service.PresenceService;
import com.sunrise.core.service.result.ResultNoArgs;
import com.sunrise.core.service.result.ResultOneArg;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Slf4j
@Controller
@RequiredArgsConstructor
public class WsMessageController {
    private final MessageService messageService;
    private final WsNotificationService wsNotify;
    private final PresenceService presenceService;
    private final ChatService chatService;

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(@DestinationVariable long chatId, @Payload WsRequests.SendMessageRequest request,
                            @WsCurrentUserId long userId, Principal principal) {

        ResultOneArg<Long> result = messageService.makePublicMessage(chatId, userId, request.tempId(), request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), "/app/chat/" + chatId + "/send");
        }
    }
    @MessageMapping("/chat/{chatId}/edit")
    public void editMessage(@DestinationVariable long chatId, @Payload WsRequests.EditMessageRequest request,
                            @WsCurrentUserId long userId, Principal principal) {

        ResultNoArgs result = messageService.editMessage(chatId, userId, request.messageId(), request.newText());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), "/app/chat/" + chatId + "/edit");
        }
    }
    @MessageMapping("/chat/{chatId}/delete")
    public void deleteMessage(@DestinationVariable long chatId, @Payload WsRequests.DeleteMessageRequest request,
                              @WsCurrentUserId long userId, Principal principal) {

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, request.messageId());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), "/app/chat/" + chatId + "/delete");
        }
    }

    @MessageMapping("/chat/{chatId}/read")
    public void markAsReadUpTo(@DestinationVariable long chatId, @Payload WsRequests.MarkAsReadRequest request,
                               @WsCurrentUserId long userId, Principal principal) {

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, request.upToMessageId());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), "/app/chat/" + chatId + "/read");
        }
    }


    @MessageMapping("/chat/{chatId}/typing/{isTyping}")
    public void typing(@DestinationVariable long chatId, @DestinationVariable boolean isTyping,
                       @WsCurrentUserId long userId) {

        presenceService.setTyping(userId, chatId, isTyping);
        wsNotify.notifyTypingStatus(chatId, userId, isTyping);
    }
    @MessageMapping("/presence")
    public void presence(@WsCurrentUserId long userId, @Payload WsRequests.PresenceRequest request) {
        presenceService.setStatus(userId, request.status());

        Long[] chatIds = chatService.getUserChatIds(userId);
        wsNotify.notifyPresenceStatus(chatIds, userId, request.status());
    }
    @MessageMapping("/ping")
    public void ping(Principal principal) {
        wsNotify.notifyPong(principal.getName());
    }
}