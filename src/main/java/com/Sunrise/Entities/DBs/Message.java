package com.Sunrise.Entities.DBs;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Cacheable(false)
@Table(name = "messages")
@AllArgsConstructor
@NoArgsConstructor
@Data
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

    @Column(name = "hidden_by_admin", nullable = false)
    private boolean hiddenByAdmin = false;
}
