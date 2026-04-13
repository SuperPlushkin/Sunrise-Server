package com.sunrise.controller;

import com.sunrise.config.annotation.WsCurrentUserId;
import com.sunrise.core.notifier.WebSocketNotifier;
import com.sunrise.core.service.ChatService;
import com.sunrise.core.service.MessageService;
import com.sunrise.core.service.PresenceService;
import com.sunrise.core.service.result.ResultNoArgs;
import com.sunrise.core.service.result.ResultOneArg;

import com.sunrise.websocket.WsRequests;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Controller
public class WebSocketController {

    private final MessageService messageService;
    private final WebSocketNotifier wsNotify;
    private final PresenceService presenceService;
    private final ChatService chatService;


    // =========================== MESSAGE ===========================
    @MessageMapping("/chats/{chatId}/messages/send")
    public void sendMessage(@DestinationVariable long chatId, @Payload WsRequests.MessageNewRequest request,
                            @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        ResultOneArg<Long> result = messageService.makePublicMessage(request.tempId(), chatId, userId, request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }
    @MessageMapping("/chats/{chatId}/messages/send-private")
    public void sendPrivateMessage(@DestinationVariable long chatId, @Payload WsRequests.MessagePrivateNewRequest request,
                            @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        ResultOneArg<Long> result = messageService.makePrivateMessage(request.tempId(), chatId, userId, request.receiverId(), request.text());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }
    @MessageMapping("/chats/{chatId}/messages/edit")
    public void editMessage(@DestinationVariable long chatId, @Payload WsRequests.MessageInfoUpdateRequest request,
                            @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        ResultNoArgs result = messageService.updateMessage(chatId, userId, request.messageId(), request.newText());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }
    @MessageMapping("/chats/{chatId}/messages/delete")
    public void deleteMessage(@DestinationVariable long chatId, @Payload WsRequests.MessageDeleteRequest request,
                              @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        ResultNoArgs result = messageService.deleteMessage(chatId, userId, request.messageId());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }

    @MessageMapping("/chats/{chatId}/messages/read")
    public void markMessagesAsReadUpTo(@DestinationVariable long chatId, @Payload WsRequests.MarkAsReadRequest request,
                                       @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        ResultNoArgs result = messageService.markMessagesUpToRead(chatId, userId, request.upToMessageId());
        if (!result.isSuccess()) {
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
        }
    }


    // =========================== ACTIONS/PRESENCE/OTHER ===========================
    @MessageMapping("/chats/{chatId}/actions/{action}")
    public void updateUserChatAction(@DestinationVariable long chatId, @DestinationVariable String action,
                                     @WsCurrentUserId long userId, Principal principal, @Header("simpDestination") String errorUrl) {

        ResultOneArg<Boolean> result = chatService.isActionsEnabledForChat(chatId, userId);
        if (!result.isSuccess()){
            wsNotify.notifyError(principal.getName(), result.getError(), errorUrl);
            return;
        }

        if (!Boolean.TRUE.equals(result.getResult())) {
            wsNotify.notifyError(principal.getName(), "Actions is not enabled for this chatType", errorUrl);
            return;
        }

        if (presenceService.updateUserAction(userId, chatId, action)){
            wsNotify.notifyUserAction(chatId, userId, action);
        }
    }

    @MessageMapping("/user-status/{status}")
    public void updateUserGlobalStatus(@WsCurrentUserId long userId, @DestinationVariable String status) {

        Set<String> sessionsToNotify = presenceService.updateUserOnlineStatus(userId, status);
        if (!sessionsToNotify.isEmpty()){
            wsNotify.notifyUserStatusChange(userId, status, sessionsToNotify);
        }
    }

    @MessageMapping("/ping")
    public void ping(Principal principal) {
        wsNotify.notifyPong(principal.getName());
    }
}