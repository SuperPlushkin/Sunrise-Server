package com.Sunrise.DTO.ServiceResults;

import java.util.Date;

@lombok.Getter
public class UserLoginResult extends ServiceResultTemplate {
    private final String jwtToken;
    private final Date expiration;
    public UserLoginResult(boolean success, String errorMessage, String jwtToken, Date expiration){
        super(success, errorMessage);
        this.jwtToken = jwtToken;
        this.expiration = expiration;
    }

    public static UserLoginResult success(String jwtToken, Date expiration) {
        return new UserLoginResult(true, null, jwtToken, expiration);
    }
    public static UserLoginResult error(String errorMessage) {
        return new UserLoginResult(false, errorMessage, null, null);
    }
}
