package com.Sunrise.Entities.Caches;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChatMember {
    private long chatId;
    private long userId;
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
}
