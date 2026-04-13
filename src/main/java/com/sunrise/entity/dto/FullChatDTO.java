package com.sunrise.entity.dto;

import com.sunrise.core.dataservice.type.ChatType;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class FullChatDTO {
    private long id;
    private String name;
    private String description;
    private ChatType chatType;
    private Long opponentId; // Только для личных чатов
    private int membersCount;
    private int deletedMembersCount;
    private MessageDTO lastMessage;
    private int unreadCount;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
}
