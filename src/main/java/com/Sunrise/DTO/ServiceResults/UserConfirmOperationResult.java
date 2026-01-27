package com.Sunrise.DTO.ServiceResults;

import java.util.Date;

@lombok.Getter
public class UserConfirmOperationResult extends ServiceResult {
    private final String jwtToken;
    private final Date expiration;
    public UserConfirmOperationResult(boolean success, String errorMessage, String jwtToken, Date expiration){
        super(success, errorMessage);
        this.jwtToken = jwtToken;
        this.expiration = expiration;
    }

    public static UserConfirmOperationResult success(String jwtToken, Date expiration) {
        return new UserConfirmOperationResult(true, null, jwtToken, expiration);
    }
    public static UserConfirmOperationResult error(String errorMessage) {
        return new UserConfirmOperationResult(false, errorMessage, null, null);
    }
}
