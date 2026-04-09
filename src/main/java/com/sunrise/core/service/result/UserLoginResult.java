package com.sunrise.core.service.result;

public record UserLoginResult(String jwtToken, java.util.Date expiration) { }