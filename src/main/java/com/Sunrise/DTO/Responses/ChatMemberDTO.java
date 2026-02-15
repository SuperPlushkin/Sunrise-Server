package com.Sunrise.DTO.Responses;

import com.Sunrise.Entities.Cache.CacheChatMember;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
public class ChatMemberDTO {
    private Long userId;
    private String username;
    private String name;
    private LocalDateTime joinedAt;
    private Boolean isAdmin = false;
    private Boolean isDeleted = false;

    public static ChatMemberDTO fromCacheChatMember(CacheChatMember member) {
        ChatMemberDTO dto = new ChatMemberDTO();
        dto.setUserId(member.getUserId());
        dto.setUsername(member.getUsername());
        dto.setName(member.getName());
        dto.setJoinedAt(member.getJoinedAt());
        dto.setIsAdmin(member.getIsAdmin());
        dto.setIsDeleted(member.getIsDeleted());
        return dto;
    }
}
