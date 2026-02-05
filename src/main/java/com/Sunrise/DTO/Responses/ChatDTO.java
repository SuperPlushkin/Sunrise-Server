package com.Sunrise.DTO.Responses;

import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheChatMember;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Setter
@Getter
public class ChatDTO {
    private Long id;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Boolean isGroup;
    private Boolean isDeleted;
    private Map<Long, CacheChatMember> chatMembers;

    public static ChatDTO fromCacheChat(CacheChat chat) {
        ChatDTO dto = new ChatDTO();
        dto.setId(chat.getId());
        dto.setName(chat.getName());
        dto.setCreatedBy(chat.getCreatedBy());
        dto.setCreatedAt(chat.getCreatedAt());
        dto.setIsGroup(chat.getIsGroup());
        dto.setIsDeleted(chat.getIsDeleted());
        dto.setChatMembers(chat.getChatMembers());
        return dto;
    }
}
