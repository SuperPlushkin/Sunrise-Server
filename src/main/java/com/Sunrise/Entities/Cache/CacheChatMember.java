package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.ChatMember;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CacheChatMember extends ChatMember {
    private Long userId;
    private LocalDateTime joinedAt;
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;

    public CacheChatMember(ChatMember chatMember){
        this.userId = chatMember.getUserId();
        this.joinedAt = chatMember.getJoinedAt();
        this.isAdmin = chatMember.getIsAdmin();
        this.isDeleted = chatMember.getIsDeleted();
    }

    public void markAsDeleted() {
        this.isDeleted = true;
    }
    public void restoreAdminRights(Boolean isAdmin) {
        this.isDeleted = false;
        this.isAdmin = isAdmin;
    }
}
