package com.Sunrise.Entities.DTOs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VerificationTokenDTO {
    private long id;
    private long userId;
    private String token;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private String tokenType;

    public VerificationTokenDTO(long id, String token, long userId, String tokenType) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.createdAt = LocalDateTime.now();
        this.tokenType = tokenType;
    }

    @JsonIgnore
    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }
}
