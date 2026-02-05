package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class HistoryOperationResult extends ServiceResultTemplate {
    private final Integer affectedMessages;

    public HistoryOperationResult(boolean success, String errorMessage, Integer affectedMessages) {
        super(success, errorMessage);
        this.affectedMessages = affectedMessages;
    }

    public static HistoryOperationResult success(Integer affectedMessages) {
        return new HistoryOperationResult(true, null, affectedMessages);
    }
    public static HistoryOperationResult error(String errorMessage) {
        return new HistoryOperationResult(false, errorMessage, null);
    }
}