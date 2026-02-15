package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.DBResults.MessageDBResult;

import java.util.List;

@lombok.Getter
public class ChatMessagesResult extends ServiceResultTemplate {
    private final List<MessageDBResult> messages;

    public ChatMessagesResult(boolean success, String errorMessage, List<MessageDBResult> messages) {
        super(success, errorMessage);

        this.messages = messages;
    }

    public static ChatMessagesResult success(List<MessageDBResult> messages) {
        return new ChatMessagesResult(true, null, messages);
    }
    public static ChatMessagesResult error(String errorMessage) {
        return new ChatMessagesResult(false, errorMessage, null);
    }
}
