package com.Sunrise.Entities.DTO;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class LightChatMemberDTO {
    private Long userId;
    private LocalDateTime joinedAt;
    private Boolean isAdmin;
    private Boolean isDeleted;
}
