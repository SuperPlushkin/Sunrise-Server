package com.Sunrise.Entities.DB;

import com.Sunrise.Entities.Cache.CacheChat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "chats")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Chat {

    @Id
    protected Long id;

    @Column(name = "name", length = 50)
    @Size(min = 4, max = 50)
    @Pattern(
        regexp = "^[a-zA-Z0-9а-яА-Я _-]+$",
        message = "Chat name can contain letters, digits, spaces, underscores, and hyphens"
    )
    protected String name;

    @Min(0)
    @Column(name = "members_count", nullable = false)
    protected Integer membersCount;

    @Min(0)
    @Column(name = "deleted_members_count", nullable = false)
    protected Integer deletedMembersCount;

    @Column(name = "created_by", nullable = false)
    protected Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_group", nullable = false)
    protected Boolean isGroup = false;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    protected Boolean isDeleted = false;

    public Chat(CacheChat cacheChat) {
        this.id = cacheChat.getId();
        this.name = cacheChat.getName();
        this.createdBy = cacheChat.getCreatedBy();
        this.createdAt = cacheChat.getCreatedAt();
        this.isGroup = cacheChat.getIsGroup();
        this.isDeleted = cacheChat.getIsDeleted();
    }

    public void setIsDeleted(boolean isDeleted){
        this.deletedAt = isDeleted ? LocalDateTime.now() : null;
        this.isDeleted = isDeleted;
    }

    public static Chat createPersonalChat(Long id, Long createdBy) {
        return new Chat(id, null, 2, 0, createdBy, LocalDateTime.now(), false, null, false);
    }
    public static Chat createGroupChat(Long id, String name, Integer members_count, Long createdBy) {
        return new Chat(id, name, members_count, 0, createdBy, LocalDateTime.now(), true, null, false);
    }
}
