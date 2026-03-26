package com.Sunrise.DTOs.ServiceResults;

import com.Sunrise.DTOs.Paginations.MessagesPageDTO;

@lombok.Getter
public class ChatMessagesResult extends ServiceResultTemplate {
    private final MessagesPageDTO pagination;

    public ChatMessagesResult(boolean success, String errorMessage, MessagesPageDTO pagination) {
        super(success, errorMessage);
        this.pagination = pagination;
    }

    public static ChatMessagesResult success(MessagesPageDTO pagination) {
        return new ChatMessagesResult(true, null, pagination);
    }
    public static ChatMessagesResult error(String errorMessage) {
        return new ChatMessagesResult(false, errorMessage, null);
    }
}
