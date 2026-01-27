package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class ChatCreationOperationResult extends ServiceResult {
    private final Long chatId;

    public ChatCreationOperationResult(boolean success, String errorMessage, Long chatId) {
        super(success, errorMessage);
        this.chatId = chatId;
    }

    public static ChatCreationOperationResult success(Long chatId) {
        return new ChatCreationOperationResult(true, null, chatId);
    }
    public static ChatCreationOperationResult error(String errorMessage) {
        return new ChatCreationOperationResult(false, errorMessage, null);
    }
}
