package com.sunrise.entity.db;

import com.sunrise.core.dataservice.type.ChatType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "chats")
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

    @Size(max = 500)
    @Column(name = "description", length = 500)
    protected String description;

    @Column(name = "chat_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatType chatType;

    @Column(name = "opponent_id")
    protected Long opponentId;

    @Min(0)
    @Column(name = "members_count", nullable = false)
    protected int membersCount;

    @Min(0)
    @Column(name = "deleted_members_count", nullable = false)
    protected int deletedMembersCount;

    @Column(name = "updated_at", nullable = false)
    protected LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    protected LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", nullable = false)
    protected long createdBy;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    protected boolean isDeleted = false;

    public void setIsDeleted(boolean isDeleted){
        this.deletedAt = isDeleted ? LocalDateTime.now() : null;
        this.isDeleted = isDeleted;
    }
    public boolean isActive() {
        return !isDeleted;
    }

    public boolean isNotPersonal(){
        return chatType.isNotPersonal();
    }
}
