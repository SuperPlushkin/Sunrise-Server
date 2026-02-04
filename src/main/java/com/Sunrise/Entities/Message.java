package com.Sunrise.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "messages")
public class Message {
    @Id
    private Long id;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    @Min(0)
    @Column(name = "read_count", nullable = false)
    private Long readCount = 0L;

    @Column(name = "hidden_by_admin", nullable = false)
    private Boolean hiddenByAdmin = false;

    public static Message create(Long id, Long senderId, Long chatId, String text) {
        return new Message(id, senderId, chatId, text, LocalDateTime.now(), 0L, false);
    }
}
