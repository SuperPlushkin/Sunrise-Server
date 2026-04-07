package com.sunrise.entity.cache;

import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CacheUser {
    private Long id;
    private String username;
    private String name;
    private String email;
    private String hashPassword;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private boolean isEnabled;
    private boolean isDeleted;

    public static CacheUser copy(CacheUser user) {
        if (user == null) return null;

        return new CacheUser(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getCreatedAt(),
            user.isEnabled(),
            user.isDeleted()
        );
    }
}
