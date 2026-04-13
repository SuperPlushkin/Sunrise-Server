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
    private LocalDateTime profileUpdatedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private int jwtVersion;
    private boolean isEnabled;
    private LocalDateTime deletedAt;
    private boolean isDeleted;

    public void setUsernameAndName(String username, String name, LocalDateTime updatedAt){
        this.username = username;
        this.name = name;
        this.profileUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }
    public void setEmail(String email, int jwtVersion, LocalDateTime updatedAt){
        this.email = email;
        this.jwtVersion = jwtVersion;
        this.updatedAt = updatedAt;
    }
    public void setPassword(String hashPassword, int jwtVersion, LocalDateTime updatedAt){
        this.hashPassword = hashPassword;
        this.jwtVersion = jwtVersion;
        this.updatedAt = updatedAt;
    }
    public void setLastLogin(LocalDateTime lastLogin){
        this.lastLogin = lastLogin;
        this.updatedAt = lastLogin;
    }
    public void enable(LocalDateTime updatedAt){
        this.isEnabled = true;
        this.profileUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }
    public void disable(LocalDateTime updatedAt){
        this.isEnabled = false;
        this.profileUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }
    public void delete(LocalDateTime updatedAt){
        this.deletedAt = updatedAt;
        this.isDeleted = true;
        this.profileUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }
    public void restore(LocalDateTime updatedAt){
        this.deletedAt = null;
        this.isDeleted = false;
        this.profileUpdatedAt = updatedAt;
        this.updatedAt = updatedAt;
    }

    public static CacheUser copy(CacheUser user) {
        if (user == null) return null;

        return new CacheUser(
            user.getId(),
            user.getUsername(),
            user.getName(),
            user.getEmail(),
            user.getHashPassword(),
            user.getLastLogin(),
            user.getProfileUpdatedAt(),
            user.getUpdatedAt(),
            user.getCreatedAt(),
            user.getJwtVersion(),
            user.isEnabled(),
            user.getDeletedAt(),
            user.isDeleted()
        );
    }
}
