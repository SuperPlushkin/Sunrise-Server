package com.Sunrise.Services.DataServices.CacheEntities;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class FullChatMember {
    private Long userId;
    private String username;
    private String name;
    private LocalDateTime joinedAt = LocalDateTime.now();
    private LocalDateTime currentJoinDate;
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;

    public static FullChatMember create(CacheUser user, CacheChatMember chatMember){
        return new FullChatMember(chatMember.getUserId(),
                user.getUsername(), user.getName(),
                chatMember.getJoinedAt(), chatMember.getCurrentJoinDate(),
                chatMember.getIsAdmin(), chatMember.getIsDeleted());
    }
}
