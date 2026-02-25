package com.Sunrise.Entities.DB;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
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

    public void setIsAdmin(boolean isAdmin){
        this.isAdmin = isAdmin;
    }
    public void setIsDeleted(boolean isDeleted){
        this.isDeleted = isDeleted;
    }


    // Конструктор для удобства
    public ChatMember(Long chatId, Long userId, LocalDateTime joinedAt, Boolean isAdmin, Boolean isDeleted) {
        this.id = new ChatMemberId(chatId, userId);
        this.isAdmin = isAdmin;
        this.joinedAt = joinedAt;
        this.isDeleted = isDeleted;
    }
    public ChatMember(Long chatId, Long userId, Boolean isAdmin) {
        this.id = new ChatMemberId(chatId, userId);
        this.isAdmin = isAdmin;
        this.joinedAt = LocalDateTime.now();
        this.isDeleted = false;
    }
}
