package com.Sunrise.Entities.DB;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_read_status")
@IdClass(MessageReadStatusId.class)
public class MessageReadStatus {
    @Id
    @ManyToOne
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime read_at = LocalDateTime.now();
}
