package com.sunrise.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sunrise.core.dataservice.type.TokenType;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class VerificationTokenDTO {
    private long id;
    private long userId;
    private String token;
    private TokenType tokenType;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;

    public VerificationTokenDTO(long id, long userId, String token, TokenType tokenType, LocalDateTime createdAt, int expireInHours) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.expiryDate = createdAt.plusHours(expireInHours);
        this.createdAt = createdAt;
        this.tokenType = tokenType;
    }

    @JsonIgnore
    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}
