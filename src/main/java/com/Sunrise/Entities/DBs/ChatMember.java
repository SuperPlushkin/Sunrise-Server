package com.Sunrise.Entities.DBs;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Cacheable(false)
@Table(name = "chat_members")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ChatMember {

    @EmbeddedId
    protected ChatMemberId id;

    @Column(name = "joined_at", nullable = false)
    protected LocalDateTime joinedAt = LocalDateTime.now();

    @Column(name = "is_admin", nullable = false)
    protected boolean isAdmin = false;

    @Column(name = "is_deleted",nullable = false)
    protected boolean isDeleted = false;


    public Long getChatId() {
        return id != null ? id.getChatId() : null;
    }

    public Long getUserId() {
        return id != null ? id.getUserId() : null;
    }
}
