package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class MessageDTO {
    private long id;
    private long chatId;
    private long senderId;
    private String text;
    private long readCount;
    private boolean readByUser;
    private LocalDateTime sentAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public static MessageDTO create(long id, long chatId, long senderId, String text, LocalDateTime createdAt) {
        return new MessageDTO(id, chatId, senderId, text, 0L, false, createdAt, createdAt, null, false);
    }
}
