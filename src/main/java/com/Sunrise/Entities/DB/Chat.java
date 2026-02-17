package com.Sunrise.Entities.DB;

import com.Sunrise.Entities.Cache.CacheChat;
import com.Sunrise.Entities.Cache.CacheUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

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

    @Column(name = "created_by", nullable = false)
    protected Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_group", nullable = false)
    protected Boolean isGroup = false;

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

    public static Chat createPersonalChat(Long id, Long createdBy) {
        return new Chat(id, null, createdBy, LocalDateTime.now(), false, false);
    }
    public static Chat createGroupChat(Long id, String name, Long createdBy) {
        return new Chat(id, name, createdBy, LocalDateTime.now(), true, false);
    }
    public static Chat copyChat(Chat chat) {
        return new Chat(chat.id, chat.name, chat.createdBy, chat.createdAt, chat.isGroup, chat.isDeleted);
    }
}
