package com.sunrise.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
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
