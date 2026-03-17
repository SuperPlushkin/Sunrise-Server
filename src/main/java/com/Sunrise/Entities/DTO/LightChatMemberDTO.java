package com.Sunrise.Entities.DTO;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class LightChatMemberDTO {
    private long userId;
    private long chatId;
    private LocalDateTime joinedAt;
    private boolean isAdmin;
    private boolean isDeleted;
}
