package com.sunrise.entity.db;

import jakarta.persistence.*;


@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "user_chat_read_state")
public class UserChatReadStatus {
    @EmbeddedId
    protected UserChatReadStatusId id;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}