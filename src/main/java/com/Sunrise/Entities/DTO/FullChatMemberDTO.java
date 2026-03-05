package com.Sunrise.Entities.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@AllArgsConstructor
public class FullChatMemberDTO {
    private Long userId;
    private String username;
    private String name;
    private LocalDateTime joinedAt;
    private Boolean isAdmin;
    private Boolean isDeleted;        // Статус членства в чате
    private Boolean userIsDeleted;    // Статус пользователя
}
