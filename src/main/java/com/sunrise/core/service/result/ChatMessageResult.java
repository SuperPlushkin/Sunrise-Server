package com.sunrise.core.service.result;

import com.sunrise.entity.dto.MessageDTO;

@lombok.Getter
public class ChatMessageResult extends ServiceResultTemplate  {
    private final MessageDTO message;

    public ChatMessageResult(boolean success, String errorMessage, MessageDTO message) {
        super(success, errorMessage);
        this.message = message;
    }

    public static ChatMessageResult success(MessageDTO message) {
        return new ChatMessageResult(true, null, message);
    }
    public static ChatMessageResult error(String errorMessage) {
        return new ChatMessageResult(false, errorMessage, null);
    }
}
