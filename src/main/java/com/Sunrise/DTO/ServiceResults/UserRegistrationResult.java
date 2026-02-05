package com.Sunrise.DTO.ServiceResults;

@lombok.Getter
public class UserRegistrationResult extends ServiceResultTemplate {
    private final String token;
    public UserRegistrationResult(boolean success, String errorMessage, String token){
        super(success, errorMessage);
        this.token = token;
    }

    public static UserRegistrationResult success(String token) {
        return new UserRegistrationResult(true, null, token);
    }
    public static UserRegistrationResult error(String errorMessage) {
        return new UserRegistrationResult(false, errorMessage, null);
    }
}