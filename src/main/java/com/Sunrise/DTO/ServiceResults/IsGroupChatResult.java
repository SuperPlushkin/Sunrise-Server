package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class IsGroupChatResult extends ServiceResult {
    private final Boolean isGroupChat;

    public IsGroupChatResult(boolean success, String errorMessage, Boolean isGroupChat) {
        super(success, errorMessage);

        this.isGroupChat = isGroupChat;
    }

    public static IsGroupChatResult success(Boolean isGroupChat) {
        return new IsGroupChatResult(true, null, isGroupChat);
    }
    public static IsGroupChatResult error(String errorMessage) {
        return new IsGroupChatResult(false, errorMessage, null);
    }
}
