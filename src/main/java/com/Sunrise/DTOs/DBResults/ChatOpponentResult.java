package com.Sunrise.DTOs.DBResults;

import java.time.LocalDateTime;

public interface ChatOpponentResult {
    Long getChatId();
    Long getUserId();
    String getUsername();
    String getName();
    String getEmail();
    String getHashPassword();
    LocalDateTime getLastLogin();
    LocalDateTime getCreatedAt();
    Boolean getIsEnabled();
    Boolean getIsDeleted();
}