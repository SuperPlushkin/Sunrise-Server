package com.sunrise.entity.db;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "message_read_status")
public class MessageReadStatus {

    @EmbeddedId
    protected MessageReadStatusId id;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt = LocalDateTime.now();
}
