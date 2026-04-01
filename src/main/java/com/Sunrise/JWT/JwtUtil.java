package com.Sunrise.JWT;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private SecretKey secretKey;

    @Value("${app.jwt.secret}")
    private String secretString;

    @Value("${app.jwt.expiration}")
    public long expiration; // in seconds

    @PostConstruct
    public void init() {
        if (secretString == null || secretString.isEmpty()) {
            throw new IllegalStateException("JWT secret must be not empty or null");
        }
        byte[] secretBytes = secretString.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes. Current length: " + secretBytes.length);
        }
        this.secretKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateToken(String username, long userId) {
        return Jwts.builder()
                .setClaims(Map.of("userId", userId))
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(secretKey)
                .compact();
    }
    public boolean validateToken(String token, String username) {
        try {
            return username.equals(extractUsername(token)) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Date getTokenExpirationTime(String token) {
        return getClaims(token).getExpiration();
    }
    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    public Long extractUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("userId", Long.class);
    }
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }
    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}