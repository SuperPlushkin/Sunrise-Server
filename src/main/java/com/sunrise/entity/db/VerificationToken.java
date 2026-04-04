package com.sunrise.entity.db;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Cacheable(false)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "verification_token")
public class VerificationToken {

    @Id
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "token_type", nullable = false)
    private String tokenType;
}
