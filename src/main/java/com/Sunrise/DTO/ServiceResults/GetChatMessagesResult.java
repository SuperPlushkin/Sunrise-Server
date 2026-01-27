package com.Sunrise.DTO.ServiceResults;

import com.Sunrise.DTO.DBResults.MessageResult;

import java.util.List;

@lombok.Getter
public class GetChatMessagesResult extends ServiceResult {
    private final List<MessageResult> messages;

    public GetChatMessagesResult(boolean success, String errorMessage, List<MessageResult> messages) {
        super(success, errorMessage);

        this.messages = messages;
    }

    public static GetChatMessagesResult success(List<MessageResult> messages) {
        return new GetChatMessagesResult(true, null, messages);
    }
    public static GetChatMessagesResult error(String errorMessage) {
        return new GetChatMessagesResult(false, errorMessage, null);
    }
}
