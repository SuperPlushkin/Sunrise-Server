package com.Sunrise.Entities.Cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheChat {
    private long id;
    private String name;
    private int membersCount;
    private int deletedMembersCount;
    private long createdBy;
    private LocalDateTime createdAt;
    private boolean isGroup;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public void setIsGroup(boolean isGroup){
        this.isGroup = isGroup;
    }
    public void setIsDeleted(boolean isDeleted){
        this.deletedAt = isDeleted ? LocalDateTime.now() : null;
        this.isDeleted = isDeleted;
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

    public void updateFromCache(CacheChat cacheChat){
        setName(cacheChat.getName());
        setDeletedMembersCount(cacheChat.getDeletedMembersCount());
        setMembersCount(cacheChat.getMembersCount());
        setCreatedBy(cacheChat.getCreatedBy());
        setIsGroup(cacheChat.isGroup());
        setCreatedAt(cacheChat.getCreatedAt());
        setDeletedAt(cacheChat.getDeletedAt());
        setIsDeleted(cacheChat.isDeleted());
    }
}
