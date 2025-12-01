package com.Sunrise.Entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

import java.time.LocalDateTime;

@Entity
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
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
    private Boolean isEnabled = true;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;


    public User(String username, String name, String email, String hashPassword) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.hashPassword = hashPassword;
    }
    public User(Long id, String username, String name, String email, String hashPassword, Boolean isEnabled) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.hashPassword = hashPassword;
        this.isEnabled = isEnabled;
    }
}