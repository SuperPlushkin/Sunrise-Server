package com.sunrise.core.notifier;

import com.sunrise.entity.dto.MessageDTO;
import com.sunrise.websocket.WsRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class WsNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;

    // ===== PUBLIC =====
    public void notifyMessageNew(long tempId, MessageDTO message) {
        sendToChatTopic(message.getChatId(), new WsRequests.NewMessageResponse(
            tempId, message.getId(), message.getChatId(), message.getSenderId(),
            message.getText(), message.getSentAt(), message.getReadCount()
        ));
    }
    public void notifyPrivateMessageNew(long tempId, long senderId, long receiverId, MessageDTO message) {
        sendToUserSessions(senderId, "/private-messages", new WsRequests.NewMessageResponse(
            tempId, message.getId(), message.getChatId(), message.getSenderId(),
            message.getText(), message.getSentAt(), message.getReadCount()
        ));
        sendToUserSessions(receiverId, "/private-messages", new WsRequests.NewMessageResponse(
            tempId, message.getId(), message.getChatId(), message.getSenderId(),
            message.getText(), message.getSentAt(), message.getReadCount()
        ));
    }
    public void notifyMessageUpdated(MessageDTO message) {
        sendToChatTopic(message.getChatId(), new WsRequests.MessageUpdatedResponse(
            message.getId(), message.getChatId(), message.getText(), LocalDateTime.now()
        ));
    }
    public void notifyMessageDeleted(long chatId, long messageId) {
        sendToChatTopic(chatId, new WsRequests.MessageDeletedResponse(messageId, chatId));
    }
    public void notifyMessageReadUpTo(long chatId, long userId, long upToMessageId, LocalDateTime readAt) {
        sendToChatTopic(chatId, new WsRequests.ReadReceiptResponse(userId, chatId, upToMessageId, readAt));
    }

    public void notifyTypingStatus(long chatId, long userId, boolean isTyping) {
        sendToChatTopic(chatId, new WsRequests.TypingResponse(userId, chatId, isTyping));
    }
    public void notifyPresenceStatus(Long[] chatIds, long userId, String status) {
        var response = new WsRequests.PresenceResponse(userId, status);
        for (long chatId : chatIds){
            sendToChatTopic(chatId, response);
        }
    }
    public void notifyPong(String sessionId) {
        sendToUserSession(sessionId, "/pong", new WsRequests.PongResponse());
    }

    public void notifyError(String sessionId, String error, String path) {
        sendToUserSession(sessionId, "/errors", new WsRequests.ErrorResponse("websocket_error", error, path));
    }


    // ===== PRIVATE =====
    private void sendToUserSession(String sessionId, String path, Object result) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/" + path, result);
    }
    private void sendToUserSessions(long userId, String path, Object result) {
        Set<String> sessions = sessionRegistry.getUserSessions(userId);
        if (sessions.isEmpty()) return;

        for (String sessionId : sessions) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/" + path, result);
        }
    }
    private void sendToChatTopic(long chatId, Object result) {
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, result);
    }
}
