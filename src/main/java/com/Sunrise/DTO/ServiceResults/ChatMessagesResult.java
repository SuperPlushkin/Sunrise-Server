package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.Entities.DTO.LightMessageDTO;

import java.util.List;

@lombok.Getter
public class ChatMessagesResult extends ServiceResultTemplate {
    private final List<LightMessageDTO> messages;

    public ChatMessagesResult(boolean success, String errorMessage, List<LightMessageDTO> messages) {
        super(success, errorMessage);

        this.messages = messages;
    }

    public static ChatMessagesResult success(List<LightMessageDTO> messages) {
        return new ChatMessagesResult(true, null, messages);
    }
    public static ChatMessagesResult error(String errorMessage) {
        return new ChatMessagesResult(false, errorMessage, null);
    }
}
