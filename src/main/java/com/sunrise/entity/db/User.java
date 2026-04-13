package com.sunrise.entity.db;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;

@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@Entity
@Cacheable(false)
@Table(name = "users")
public class User {

    @Id
    private Long id;

    @Column(name = "username", unique = true, nullable = false, length = 30)
    private String username;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Email
    @Column(name = "email", nullable = false, length = 60)
    private String email;

    @Column(name = "hash_password", nullable = false, length = 64)
    private String hashPassword;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "profile_updated_at", nullable = false)
    private LocalDateTime profileUpdatedAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "jwt_version", nullable = false)
    private int jwtVersion = 1;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}