package com.Sunrise.DTO.Responses;

import com.Sunrise.Entities.Cache.CacheChatMember;
import com.Sunrise.Entities.DB.ChatMember;
import com.Sunrise.Entities.DB.User;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ChatMemberDTO {
    private Long userId;
    private String username;
    private String name;
    private LocalDateTime joinedAt;
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;

    public ChatMemberDTO(ChatMember member, User user) {
        this.setUserId(member.getUserId());
        this.setUsername(user.getUsername());
        this.setName(user.getName());
        this.setJoinedAt(member.getJoinedAt());
        this.setIsAdmin(member.getIsAdmin());
        this.setIsDeleted(member.getIsDeleted());
    }
}
