package com.Sunrise.Entities.DTO;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class FullChatDTO {
    private long id;
    private String name;
    private int membersCount;
    private int deletedMembersCount;
    private boolean isGroup;
    private long createdBy;
    private LocalDateTime createdAt;
    private Long otherUserId; // Только для личных чатов
    private LightMessageDTO lastMessage;
    //private Integer unreadCount;
    private boolean isDeleted;
    private LocalDateTime deletedAt;


}
