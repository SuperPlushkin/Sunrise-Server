package com.sunrise.entity.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
public class MessageReadStatusId implements Serializable {

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}