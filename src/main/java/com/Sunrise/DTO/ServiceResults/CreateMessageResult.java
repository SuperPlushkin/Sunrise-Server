package com.Sunrise.DTO.ServiceResults;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateMessageResult extends ServiceResult {
    private final Long messageId;
    private final LocalDateTime sentAt;

    public CreateMessageResult(boolean success, String errorMessage, Long messageId, LocalDateTime sentAt) {
        super(success, errorMessage);
        this.messageId = messageId;
        this.sentAt = sentAt;
    }

    public static CreateMessageResult success(Long messageId, LocalDateTime sentAt) {
        return new CreateMessageResult(true, null, messageId, sentAt);
    }

    public static CreateMessageResult error(String errorMessage) {
        return new CreateMessageResult(false, errorMessage, null, null);
    }
}
