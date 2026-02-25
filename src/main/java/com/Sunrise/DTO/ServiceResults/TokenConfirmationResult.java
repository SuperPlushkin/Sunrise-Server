package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class TokenConfirmationResult extends ServiceResultTemplate {
    private final String operationText;

    public TokenConfirmationResult(boolean success, String errorMessage, String operationText){
        super(success, errorMessage);
        this.operationText = operationText;
    }

    public static TokenConfirmationResult success(String operationText) {
        return new TokenConfirmationResult(true, null, operationText);
    }
    public static TokenConfirmationResult error(String errorMessage) {
        return new TokenConfirmationResult(false, errorMessage, null);
    }
}
