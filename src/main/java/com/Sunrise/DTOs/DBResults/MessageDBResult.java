package com.Sunrise.DTOs.DBResults;

import java.time.LocalDateTime;

public interface MessageDBResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getText();
    LocalDateTime getSentAt();
    Long getReadCount();
    Boolean getIsHiddenByAdmin();
}