package com.sunrise.entity.cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChatMember {
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

    public boolean isActive() {
        return !isDeleted;
    }
    public static CacheChatMember copy(CacheChatMember member){
        if (member == null) return null;

        return new CacheChatMember(
            member.getChatId(),
            member.getUserId(),
            member.getTag(),
            member.getSettingsUpdatedAt(),
            member.getUpdatedAt(),
            member.getJoinedAt(),
            member.isPinned(),
            member.isAdmin(),
            member.getDeletedAt(),
            member.isDeleted()
        );
    }
}
