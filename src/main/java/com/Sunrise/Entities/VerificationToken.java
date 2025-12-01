package com.Sunrise.Entities;

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
    private Long user_id;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "token_type", nullable = false)
    private String tokenType;

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }

    public VerificationToken(Long id, String token, Long user_id, String tokenType) {
        this.id = id;
        this.token = token;
        this.user_id = user_id;
        this.expiryDate = LocalDateTime.now().plusHours(24);
        this.createdAt = LocalDateTime.now();
        this.tokenType = tokenType;
    }

    public static VerificationToken copyVerificationToken(VerificationToken verificationToken) {
        return new VerificationToken(verificationToken.id, verificationToken.token, verificationToken.user_id, verificationToken.expiryDate, verificationToken.createdAt, verificationToken.tokenType);
    }
}
