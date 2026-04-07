package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class FullChatMemberDTO {
    private long chatId;
    private long userId;
    private String username;
    private String name;
    private LocalDateTime joinedAt;
    private boolean isAdmin;
    private boolean isDeleted;        // Статус членства в чате
    private boolean userIsDeleted;    // Статус пользователя
}
