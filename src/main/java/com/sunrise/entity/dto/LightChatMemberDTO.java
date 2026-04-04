package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class LightChatMemberDTO {
    private long chatId;
    private long userId;
    private LocalDateTime joinedAt;
    private boolean isAdmin;
    private boolean isDeleted;

    public static LightChatMemberDTO create(long chatId, long userId, boolean isAdmin){
        return new LightChatMemberDTO(chatId, userId, LocalDateTime.now(), isAdmin, false);
    }
}
