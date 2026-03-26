package com.Sunrise.Entities.DBs;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "user_chat_read_state")
public class UserChatReadStatus {
    @EmbeddedId
    protected UserChatReadStatusId id;

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;
}