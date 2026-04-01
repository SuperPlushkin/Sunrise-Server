package com.Sunrise.Entities.DBs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Cacheable(false)
@Table(name = "message_read_status")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageReadStatus {

    @EmbeddedId
    protected MessageReadStatusId id;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;
}
