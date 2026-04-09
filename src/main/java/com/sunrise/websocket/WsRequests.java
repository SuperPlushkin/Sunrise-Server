package com.sunrise.websocket;

import java.time.LocalDateTime;
import java.util.Map;

public final class WsRequests {

    // ---------- Client -> Server ----------
    public record SendMessageRequest(Long tempId, String text) {}
    public record EditMessageRequest(Long messageId, String newText) {}
    public record DeleteMessageRequest(Long messageId) {}
    public record MarkAsReadRequest(Long upToMessageId) {}
    public record PresenceRequest(String status) {} // "online", "offline", "away"


    // ---------- Server -> Client ----------
    public record NewMessageResponse(long tempId, long messageId, long chatId, long senderId, String text, LocalDateTime sentAt, long readCount) {}
    public record MessageUpdatedResponse(long messageId, long chatId, String newText, LocalDateTime updatedAt) {}
    public record MessageDeletedResponse(long messageId, long chatId) {}

    public record ReadReceiptResponse(long userId, long chatId, long upToMessageId, LocalDateTime readAt) {}

    public record TypingResponse(long userId, long chatId, boolean isTyping) {}
    public record PresenceResponse(long userId, String status) {}

    public record ParticipantJoinedResponse(long chatId, long userId, String username, String name) {}
    public record ParticipantLeftResponse(long chatId, long userId, String username, String name) {}


    public record ChatUpdatedResponse(long chatId, Map<String, Object> metadata) {}

    public record ErrorResponse(String error, String message, String path) {}
    public record PongResponse() {}
}