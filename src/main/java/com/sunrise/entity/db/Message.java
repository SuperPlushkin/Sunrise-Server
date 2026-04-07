package com.sunrise.entity.db;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "messages")
public class Message {
    @Id
    private long id;

    @Column(name = "chat_id", nullable = false)
    private long chatId;

    @Column(name = "sender_id", nullable = false)
    private long senderId;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Min(0)
    @Column(name = "read_count", nullable = false)
    private long readCount = 0L;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public boolean isActive(){
        return !isDeleted;
    }
}
