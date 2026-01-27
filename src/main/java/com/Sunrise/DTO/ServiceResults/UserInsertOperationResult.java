package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class UserInsertOperationResult extends ServiceResult {
    private final String token;
    public UserInsertOperationResult(boolean success, String errorMessage, String token){
        super(success, errorMessage);
        this.token = token;
    }

    public static UserInsertOperationResult success(String token) {
        return new UserInsertOperationResult(true, null, token);
    }
    public static UserInsertOperationResult error(String errorMessage) {
        return new UserInsertOperationResult(false, errorMessage, null);
    }
}