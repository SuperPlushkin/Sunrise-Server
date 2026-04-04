package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class FullChatDTO {
    private long id;
    private String name;
    private boolean isGroup;
    private Long opponentId; // Только для личных чатов
    private int membersCount;
    private int deletedMembersCount;
    private LightMessageDTO lastMessage;
    private LocalDateTime createdAt;
    private long createdBy;
    //private Integer unreadCount;
    private LocalDateTime deletedAt;
    private boolean isDeleted;
}
