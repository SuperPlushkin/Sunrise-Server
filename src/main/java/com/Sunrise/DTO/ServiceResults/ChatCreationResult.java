package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class ChatCreationResult extends ServiceResultTemplate {
    private final Long chatId;

    public ChatCreationResult(boolean success, String errorMessage, Long chatId) {
        super(success, errorMessage);
        this.chatId = chatId;
    }

    public static ChatCreationResult success(Long chatId) {
        return new ChatCreationResult(true, null, chatId);
    }
    public static ChatCreationResult error(String errorMessage) {
        return new ChatCreationResult(false, errorMessage, null);
    }
}
