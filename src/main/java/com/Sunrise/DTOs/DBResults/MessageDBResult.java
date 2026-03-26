package com.Sunrise.DTOs.DBResults;

public interface MessageDBResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getText();
    String getSentAt();
    Long getReadCount();
    Boolean getIsHiddenByAdmin();
}