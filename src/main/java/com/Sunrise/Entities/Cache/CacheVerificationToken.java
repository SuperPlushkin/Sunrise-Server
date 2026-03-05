package com.Sunrise.Entities.Cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheVerificationToken {
    private Long id;
    private String token;
    private Long userId;
    private LocalDateTime expiryDate;
    private LocalDateTime createdAt;
    private String tokenType;
}
