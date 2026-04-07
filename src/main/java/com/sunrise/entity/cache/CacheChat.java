package com.sunrise.entity.cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChat {

    private long id;
    private String name;
    private boolean isGroup;
    private Long opponentId;
    private int membersCount;
    private int deletedMembersCount;
    private LocalDateTime createdAt;
    private long createdBy;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public boolean isActive() {
        return !isDeleted;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
        this.isDeleted = true;
    }

    public void restore() {
        this.deletedAt = null;
        this.isDeleted = false;
    }

    public void updateFromCache(CacheChat cacheChat) {
        this.name = cacheChat.getName();
        this.isGroup = cacheChat.isGroup();
        this.opponentId = cacheChat.getOpponentId();
        this.deletedMembersCount = cacheChat.getDeletedMembersCount();
        this.membersCount = cacheChat.getMembersCount();
        this.createdBy = cacheChat.getCreatedBy();
        this.createdAt = cacheChat.getCreatedAt();
        this.deletedAt = cacheChat.getDeletedAt();
        this.isDeleted = cacheChat.isDeleted();
    }

    public static CacheChat copy(CacheChat chat) {
        if (chat == null) return null;

        return new CacheChat(
            chat.getId(),
            chat.getName(),
            chat.isGroup(),
            chat.getOpponentId(),
            chat.getMembersCount(),
            chat.getDeletedMembersCount(),
            chat.getCreatedAt(),
            chat.getCreatedBy(),
            chat.getDeletedAt(),
            chat.isDeleted()
        );
    }
}