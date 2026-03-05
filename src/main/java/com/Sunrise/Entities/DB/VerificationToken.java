package com.Sunrise.Entities.DB;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_token")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class VerificationToken {

    @Id
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "token_type", nullable = false)
    private String tokenType;
}
