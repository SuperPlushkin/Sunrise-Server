package com.Sunrise.Entities.Cache;

import com.Sunrise.Entities.DB.Chat;

@lombok.Getter
@lombok.Setter
public class CacheChat extends Chat {
    public CacheChat(Long id, String name, Long createdBy, Boolean isGroup){
        super();
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
        this.isGroup = isGroup;
    }
    public CacheChat(Chat chat) {
        super();
        this.setId(chat.getId());
        this.setName(chat.getName());
        this.setDeletedMembersCount(chat.getDeletedMembersCount());
        this.setMembersCount(chat.getMembersCount());
        this.setCreatedBy(chat.getCreatedBy());
        this.setIsGroup(chat.getIsGroup());
        this.setCreatedAt(chat.getCreatedAt());
        this.setIsDeleted(chat.getIsDeleted());
    }

    public void updateFromEntity(Chat chat) {
        this.setName(chat.getName());
        this.setDeletedMembersCount(chat.getDeletedMembersCount());
        this.setMembersCount(chat.getMembersCount());
        this.setCreatedBy(chat.getCreatedBy());
        this.setIsGroup(chat.getIsGroup());
        this.setCreatedAt(chat.getCreatedAt());
        this.setIsDeleted(chat.getIsDeleted());
    }
}
