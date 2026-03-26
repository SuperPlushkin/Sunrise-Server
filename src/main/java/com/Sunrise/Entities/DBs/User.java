package com.Sunrise.Entities.DBs;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Data
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "enabled", nullable = false)
    private boolean isEnabled = true;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;
}