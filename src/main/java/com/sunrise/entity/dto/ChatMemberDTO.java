package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class ChatMemberDTO {
    private long chatId;
    private long userId;
    private String tag;
    private LocalDateTime settingsUpdatedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime joinedAt;
    private boolean isPinned;
    private boolean isAdmin;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public static ChatMemberDTO create(long chatId, long userId, LocalDateTime createdAt, boolean isAdmin) {
        return new ChatMemberDTO(chatId, userId, null, createdAt, createdAt, createdAt, false, isAdmin, null, false);
    }
}
