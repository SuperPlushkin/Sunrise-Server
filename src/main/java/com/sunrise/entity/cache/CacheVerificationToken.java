package com.sunrise.entity.cache;

import com.sunrise.core.dataservice.type.TokenType;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheVerificationToken {
    private long id;
    private long userId;
    private String token;
    private TokenType tokenType;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;

    public static CacheVerificationToken copy(CacheVerificationToken token) {
        if (token == null) return null;

        return new CacheVerificationToken(
            token.getId(),
            token.getUserId(),
            token.getToken(),
            token.getTokenType(),
            token.getExpiryDate(),
            token.getCreatedAt()
        );
    }
}
