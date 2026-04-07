package com.sunrise.entity.db;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "chat_members")
public class ChatMember {

    @EmbeddedId
    protected ChatMemberId id;

    @Column(name = "joined_at", nullable = false)
    protected LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "is_admin", nullable = false)
    protected boolean isAdmin = false;

    public boolean isActive() {
        return !isDeleted;
    }

    @Column(name = "is_deleted",nullable = false)
    protected boolean isDeleted = false;


    public Long getChatId() {
        return id != null ? id.getChatId() : null;
    }

    public Long getUserId() {
        return id != null ? id.getUserId() : null;
    }
}
