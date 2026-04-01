package com.Sunrise.Entities.DBs;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Cacheable(false)
@Table(name = "user_chat_read_state")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserChatReadStatus {
    @EmbeddedId
    protected UserChatReadStatusId id;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}