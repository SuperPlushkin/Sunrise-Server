package com.Sunrise.Entities.Cache;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class CacheMessage {
    private long id;
    private long senderId;
    private long chatId;
    private String text;
    private LocalDateTime sentAt;
    private long readCount;
    private boolean hiddenByAdmin;

    private final Set<Long> readByUserIds = ConcurrentHashMap.newKeySet();

    public CacheMessage(long id, long senderId, long chatId, String text, LocalDateTime sentAt, long readCount, Collection<Long> readByUsers, boolean hiddenByAdmin) {
        this.id = id;
        this.senderId = senderId;
        this.chatId = chatId;
        this.text = text;
        this.sentAt = sentAt;
        this.readCount = readCount;
        this.readByUserIds.addAll(readByUsers);
        this.hiddenByAdmin = hiddenByAdmin;
    }


    public void markAsReadByUser(long userId) {
        if (readByUserIds.add(userId)) {
            readCount++;
        }
    }
    public boolean isReadByUser(long userId) {
        return readByUserIds.contains(userId);
    }
    public boolean isReadByExcludeUser(long userId) {
        return readByUserIds.stream().anyMatch(id -> id != userId);
    }

    public void incrementReadCount() {
        this.readCount++;
    }
}