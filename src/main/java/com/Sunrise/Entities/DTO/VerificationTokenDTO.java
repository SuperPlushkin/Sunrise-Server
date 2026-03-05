package com.Sunrise.Entities.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class VerificationTokenDTO {
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private String tokenType;

    public VerificationTokenDTO(Long id, String token, Long userId, String tokenType) {
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
