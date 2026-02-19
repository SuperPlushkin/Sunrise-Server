package com.Sunrise.DTO.Responses;

import com.Sunrise.Entities.DB.Chat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ChatDTO {
    private Long id;
    private String name;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Boolean isGroup;
    private Boolean isDeleted;

    public ChatDTO(Chat chat) {
        this.setId(chat.getId());
        this.setName(chat.getName());
        this.setCreatedBy(chat.getCreatedBy());
        this.setCreatedAt(chat.getCreatedAt());
        this.setIsGroup(chat.getIsGroup());
        this.setIsDeleted(chat.getIsDeleted());
    }
}
