package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.Chat;

public class CacheChat extends Chat {
    public CacheChat(Chat chat) {
        super();
        this.setId(chat.getId());
        this.setName(chat.getName());
        this.setDeletedMembersCount(chat.getDeletedMembersCount());
        this.setMembersCount(chat.getMembersCount());
        this.setCreatedBy(chat.getCreatedBy());
        this.setIsGroup(chat.getIsGroup());
        this.setCreatedAt(chat.getCreatedAt());
        this.setDeletedAt(chat.getDeletedAt());
        this.setIsDeleted(chat.getIsDeleted());
    }
    public void updateFromEntity(Chat chat) {
        this.setName(chat.getName());
        this.setDeletedMembersCount(chat.getDeletedMembersCount());
        this.setMembersCount(chat.getMembersCount());
        this.setCreatedBy(chat.getCreatedBy());
        this.setIsGroup(chat.getIsGroup());
        this.setCreatedAt(chat.getCreatedAt());
        this.setDeletedAt(chat.getDeletedAt());
        this.setIsDeleted(chat.getIsDeleted());
    }
}
