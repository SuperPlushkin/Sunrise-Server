package com.sunrise.entity.db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Data
@EqualsAndHashCode
public class UserChatReadStatusId {
    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
