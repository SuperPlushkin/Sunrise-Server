package com.sunrise.core.service.result;

import com.sunrise.entity.dto.LightMessageDTO;

@lombok.Getter
public class ChatMessageResult extends ServiceResultTemplate  {
    private final LightMessageDTO message;

    public ChatMessageResult(boolean success, String errorMessage, LightMessageDTO message) {
        super(success, errorMessage);
        this.message = message;
    }

    public static ChatMessageResult success(LightMessageDTO message) {
        return new ChatMessageResult(true, null, message);
    }
    public static ChatMessageResult error(String errorMessage) {
        return new ChatMessageResult(false, errorMessage, null);
    }
}
