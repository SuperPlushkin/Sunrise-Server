package com.Sunrise.Entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
public class FullChatMemberDTO {
    private long userId;
    private long chatId;
    private String username;
    private String name;
    private LocalDateTime joinedAt;
    private boolean isAdmin;
    private boolean isDeleted;        // Статус членства в чате
    private boolean userIsDeleted;    // Статус пользователя
}
