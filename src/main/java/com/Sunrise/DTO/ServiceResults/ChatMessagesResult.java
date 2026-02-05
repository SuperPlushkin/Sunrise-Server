package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.DBResults.GetMessageDBResult;

import java.util.List;

@lombok.Getter
public class ChatMessagesResult extends ServiceResultTemplate {
    private final List<GetMessageDBResult> messages;

    public ChatMessagesResult(boolean success, String errorMessage, List<GetMessageDBResult> messages) {
        super(success, errorMessage);

        this.messages = messages;
    }

    public static ChatMessagesResult success(List<GetMessageDBResult> messages) {
        return new ChatMessagesResult(true, null, messages);
    }
    public static ChatMessagesResult error(String errorMessage) {
        return new ChatMessagesResult(false, errorMessage, null);
    }
}
