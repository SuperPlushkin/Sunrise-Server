package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class LightMessageDTO {
    private long id;
    private long chatId;
    private long senderId;
    private String text;
    private LocalDateTime sentAt;
    private long readCount;
    private boolean readByUser;
    private boolean hiddenByAdmin;

    public static LightMessageDTO create(long id, long chatId, long senderId, String text) {
        return new LightMessageDTO(id, chatId, senderId, text, LocalDateTime.now(), 0L, false, false);
    }
}
