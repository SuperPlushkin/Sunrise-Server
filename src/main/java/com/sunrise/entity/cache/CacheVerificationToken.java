package com.sunrise.entity.cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheVerificationToken {
    private long id;
    private long userId;
    private String token;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private String tokenType;

    public static CacheVerificationToken copy(CacheVerificationToken token) {
        if (token == null) return null;

        return new CacheVerificationToken(
            token.getId(),
            token.getUserId(),
            token.getToken(),
            token.getExpiryDate(),
            token.getCreatedAt(),
            token.getTokenType()
        );
    }
}
