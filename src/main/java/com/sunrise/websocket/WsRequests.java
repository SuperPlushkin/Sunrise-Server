package com.sunrise.websocket;

import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

public final class WsRequests {

    // ---------- Client -> Server ----------
    public record MessageNewRequest(Long tempId, String text) {}
    public record MessagePrivateNewRequest(Long tempId, Long receiverId, String text) {}
    public record MessageInfoUpdateRequest(Long messageId, String newText) {}
    public record MessageDeleteRequest(Long messageId) {}
    public record MarkAsReadRequest(Long upToMessageId) {}


    // ---------- Server -> Client ----------
    public record MessageNewResponse(long tempId, long messageId, long chatId, long senderId, String text, long readCount, LocalDateTime sentAt, LocalDateTime updatedAt, LocalDateTime deletedAt, boolean isDeleted) {}
    public record MessagePrivateNewResponse(long tempId, long messageId, long chatId, long senderId, String text, LocalDateTime sentAt) {}
    public record MessageUpdateResponse(long messageId, long chatId, String newText, LocalDateTime updatedAt) {}
    public record MessageDeleteResponse(long messageId, long chatId, LocalDateTime deletedAt) {}
    public record MessagesReadUpToResponse(long userId, long chatId, long upToMessageId, LocalDateTime readAt) {}

    public record ChatNewResponse(long tempId, long chatId, String name, String description, ChatType chatType, Long opponentId, int membersCount, LocalDateTime updatedAt, LocalDateTime createdAt, long createdBy) {}
    public record ChatInfoUpdateResponse(long chatId, String newName, String newDescription, LocalDateTime updatedAt) {}
    public record ChatTypeUpdateResponse(long chatId, ChatType newType, LocalDateTime updatedAt) {}
    public record ChatDeleteResponse(long chatId, LocalDateTime deletedAt) {}

    public record ChatMemberNewResponse(long chatId, long userId, LocalDateTime updatedAt, LocalDateTime joinedAt, boolean isAdmin, LocalDateTime deletedAt, boolean isDeleted) {}
    public record ChatMemberInfoUpdateResponse(long chatId, long userId, String tag, LocalDateTime updatedAt) {}
    public record ChatMemberAdminRightsUpdateResponse(long chatId, long userId, boolean isAdmin, LocalDateTime updatedAt) {}
    public record ChatMemberSettingsUpdateResponse(long chatId, long userId, boolean isPinned, LocalDateTime updatedAt) {}
    public record ChatMemberDeleteResponse(long chatId, long userId, LocalDateTime deletedAt) {}

    public record UserProfileUpdatedResponse(long userId, String newUsername, String newName, LocalDateTime updatedAt) {}
    public record UserEmailUpdateResponse(long chatId, ChatType newType, LocalDateTime updatedAt) {}
    public record UserPasswordUpdateResponse(long chatId, ChatType newType, LocalDateTime updatedAt) {}
    public record UserDisabledResponse(long userId, LocalDateTime disabledAt) {}
    public record UserDeleteResponse(long userId, LocalDateTime deletedAt) {}

    public record UserStatusResponse(long userId, String newStatus) {}
    public record UserChatActionResponse(long userId, long chatId, String action) {}
    public record PongResponse() {}

    public record ErrorResponse(String error, String message, String path) {}
}