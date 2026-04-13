package com.sunrise.entity.cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheMessage {
    private long id;
    private long chatId;
    private long senderId;
    private LocalDateTime sentAt;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public void delete(LocalDateTime updatedAt) {
        this.isDeleted = true;
        this.deletedAt = updatedAt;
    }
    public void restore() {
        this.isDeleted = false;
        this.deletedAt = null;
    }

    public boolean isActive() {
        return !isDeleted;
    }

    public static CacheMessage copy(CacheMessage message) {
        if (message == null) return null;

        return new CacheMessage(
            message.getId(),
            message.getChatId(),
            message.getSenderId(),
            message.getSentAt(),
            message.getDeletedAt(),
            message.isDeleted()
        );
    }
}