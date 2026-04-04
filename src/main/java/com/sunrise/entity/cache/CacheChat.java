package com.sunrise.entity.cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
public class CacheChat {

    // Chat fields
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

    private CacheMessage newestMessage;

    private final LocalDateTime cachedAt = LocalDateTime.now();

    public CacheChat(long id, String name, boolean isGroup, Long opponentId,
                     int membersCount, int deletedMembersCount,
                     LocalDateTime createdAt, long createdBy,
                     LocalDateTime deletedAt, boolean isDeleted, CacheMessage newestMessage) {
        this.id = id;
        this.name = name;
        this.isGroup = isGroup;
        this.opponentId = opponentId;
        this.membersCount = membersCount;
        this.deletedMembersCount = deletedMembersCount;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.deletedAt = deletedAt;
        this.isDeleted = isDeleted;
        this.newestMessage = newestMessage;
    }

    public boolean isActive() {
        return !isDeleted;
    }
    public void delete(){
        this.deletedAt = LocalDateTime.now();
        this.isDeleted = true;
    }
    public void restore(){
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
        this.newestMessage = cacheChat.getNewestMessage();
    }


    // ========== ДЕЙСТВИЯ С СООБЩЕНИЯМИ ==========

    public void updateNewestMessageIsDeletedIfHasId(long messageId, boolean isDeleted) {
        if (newestMessage != null && newestMessage.getId() == messageId) {
            newestMessage.setHiddenByAdmin(isDeleted);
        }
    }
}
