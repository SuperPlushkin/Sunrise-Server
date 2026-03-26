package com.Sunrise.Entities.Caches;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CacheMessage {
    private long id;
    private long chatId;
    private long senderId;
    private String text;
    private LocalDateTime sentAt;
    private long readCount;
    private boolean hiddenByAdmin;

    public CacheMessage(long id, long chatId, long senderId, String text, LocalDateTime sentAt, long readCount, boolean hiddenByAdmin) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.text = text;
        this.sentAt = sentAt;
        this.readCount = readCount;
        this.hiddenByAdmin = hiddenByAdmin;
    }

    public void incrementReadCount() {
        this.readCount++;
    }
}