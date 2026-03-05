package com.Sunrise.Entities.Cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChatMember {
    private long userId;
    private long chatId;
    private LocalDateTime joinedAt;
    private boolean isAdmin;
    private boolean isDeleted;

    public void setIsAdmin(boolean isAdmin){
        this.isAdmin = isAdmin;
    }
    public void setIsDeleted(boolean isDeleted){
        this.isDeleted = isDeleted;
    }
    public boolean isActive() {
        return !isDeleted;
    }

    public CacheChatMember(long userId, long chatId, boolean isAdmin){
        this.userId = userId;
        this.chatId = chatId;
        this.joinedAt = LocalDateTime.now();
        this.isAdmin = isAdmin;
        this.isDeleted = false;
    }
}
