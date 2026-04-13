package com.sunrise.core.notifier;

import com.sunrise.core.dataservice.type.ChatType;
import com.sunrise.entity.dto.LightChatDTO;
import com.sunrise.entity.dto.ChatMemberDTO;
import com.sunrise.entity.dto.MessageDTO;
import com.sunrise.websocket.WsRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class WebSocketNotifier { // TODO: Добавить @Async если большие операции будут

    private final SimpMessagingTemplate messagingTemplate;
    private final SessionRegistry sessionRegistry;


    // ======================= MESSAGE ============================
    public void notifyMessageNew(long tempId, MessageDTO message) {
        sendToChatTopic(message.getChatId(), new WsRequests.MessageNewResponse(
            tempId, message.getId(), message.getChatId(), message.getSenderId(),
            message.getText(), message.getReadCount(), message.getSentAt(),
            message.getUpdatedAt(), message.getDeletedAt(), message.isDeleted()
        ));
    }
    public void notifyMessagePrivateNew(long tempId, MessageDTO message, long receiverId) {
        var response = new WsRequests.MessagePrivateNewResponse(
            tempId, message.getId(), message.getChatId(), message.getSenderId(),
            message.getText(), message.getSentAt()
        );
        for (long userId : List.of(message.getSenderId(), receiverId)) {
            sendToUserSessions(userId, "/private-messages", response);
        }
    }
    public void notifyMessageInfoUpdated(long chatId, long messageId, String newText, LocalDateTime updatedAt) {
        sendToChatTopic(chatId, new WsRequests.MessageUpdateResponse(
            messageId, chatId, newText, updatedAt
        ));
    }
    public void notifyMessageDeleted(long chatId, long messageId, LocalDateTime deletedAt) {
        sendToChatTopic(chatId, new WsRequests.MessageDeleteResponse(messageId, chatId, deletedAt));
    }
    public void notifyMessageReadUpTo(long chatId, long userId, long upToMessageId, LocalDateTime readAt) {
        sendToChatTopic(chatId, new WsRequests.MessagesReadUpToResponse(userId, chatId, upToMessageId, readAt));
    }


    // ========================= CHAT =============================
    public void notifyChatNew(long tempId, LightChatDTO chat, Set<Long> userIdsToNotify) {
        var response = new WsRequests.ChatNewResponse(
            tempId, chat.getId(), chat.getName(), chat.getDescription(),
            chat.getChatType(), chat.getOpponentId(), chat.getMembersCount(),
            chat.getUpdatedAt(), chat.getCreatedAt(), chat.getCreatedBy()
        );
        for (long userId : userIdsToNotify){
            sendToUserSessions(userId, "/chats", response);
        }
    }
    public void notifyChatInfoUpdated(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatInfoUpdateResponse(
            chatId, newName, newDescription, updatedAt
        ));
    }
    public void notifyChatTypeUpdated(long chatId, ChatType newChatType, LocalDateTime updatedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatTypeUpdateResponse(chatId, newChatType, updatedAt));
    }
    public void notifyChatDeleted(long chatId, LocalDateTime deletedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatDeleteResponse(chatId, deletedAt));
    }


    // ===================== CHAT-MEMBER ===========================
    public void notifyChatMemberNew(ChatMemberDTO chatMember) {
        sendToChatTopic(chatMember.getChatId(), new WsRequests.ChatMemberNewResponse(
            chatMember.getChatId(), chatMember.getUserId(),
            chatMember.getUpdatedAt(), chatMember.getJoinedAt(),
            chatMember.isAdmin(), chatMember.getDeletedAt(), chatMember.isDeleted()
        ));
    }
    public void notifyChatMembersNew(Collection<ChatMemberDTO> chatMembers) {
        for (ChatMemberDTO chatMember : chatMembers) {
            sendToChatTopic(chatMember.getChatId(), new WsRequests.ChatMemberNewResponse(
                chatMember.getChatId(), chatMember.getUserId(),
                chatMember.getUpdatedAt(), chatMember.getJoinedAt(),
                chatMember.isAdmin(), chatMember.getDeletedAt(), chatMember.isDeleted()
            ));
        }
    }
    public void notifyChatMemberInfoUpdated(long chatId, long userId, String tag, LocalDateTime updatedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatMemberInfoUpdateResponse(chatId, userId, tag, updatedAt));
    }
    public void notifyChatMemberAdminRightsUpdated(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatMemberAdminRightsUpdateResponse(chatId, userId, isAdmin, updatedAt));
    }
    public void notifyChatMemberSettingsUpdated(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatMemberSettingsUpdateResponse(chatId, userId, isPinned, updatedAt));
    }
    public void notifyChatMemberDeleted(long chatId, long userId, LocalDateTime deletedAt) {
        sendToChatTopic(chatId, new WsRequests.ChatMemberDeleteResponse(chatId, userId, deletedAt));
    }


    // ========================= USER =============================
    public void notifyUserProfileUpdated(long userId, String newUsername, String newName, LocalDateTime updatedAt) {

    }
    public void notifyUserEmailUpdate(long userId, LocalDateTime deletedAt) {

    }
    public void notifyUserPasswordUpdate(long userId, LocalDateTime deletedAt) {

    }
    public void notifyUserDisabled(long userId, LocalDateTime deletedAt) {

    }
    public void notifyUserDeleted(long userId, LocalDateTime deletedAt) {

    }


    // ================= PRESENCE/STATUS/OTHER ====================
    public void notifyUserStatusChange(long userId, String newStatus, Set<String> userSessionsToNotify) {
        var response = new WsRequests.UserStatusResponse(userId, newStatus);
        for (String sessionId : userSessionsToNotify) {
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/user-status", response);
        }
    }
    public void notifyUserAction(long chatId, long userId, String action) {
        sendToChatTopic(chatId, new WsRequests.UserChatActionResponse(userId, chatId, action));
    }
    public void notifyPong(String sessionId) {
        sendToUserSession(sessionId, "/pong", new WsRequests.PongResponse());
    }

    public void notifyError(String sessionId, String error, String errorUrl) {
        sendToUserSession(sessionId, "/errors", new WsRequests.ErrorResponse("websocket_error", error, errorUrl));
    }


    // ===== PRIVATE =====
    private void sendToUserSession(String sessionId, String path, Object result) {
        messagingTemplate.convertAndSendToUser(sessionId, "/queue" + path, result);
    }
    private void sendToUserSessions(long userId, String path, Object result) {
        Set<String> sessions = sessionRegistry.getUserSessions(userId);
        if (sessions.isEmpty()) return;

        for (String sessionId : sessions) {
            sendToUserSession(sessionId, "/queue" + path, result);
        }
    }
    private void sendToChatTopic(long chatId, Object result) {
        messagingTemplate.convertAndSend("/topic/chats/" + chatId, result);
    }
}
