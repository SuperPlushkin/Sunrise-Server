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
}
