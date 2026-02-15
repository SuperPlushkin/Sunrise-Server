package com.Sunrise.DTO.DBResults;

public interface MessageDBResult {
    Long getMessageId();
    Long getSenderId();
    String getSenderUsername();
    String getText();
    String getSentAt();
    Long getReadCount();
    Boolean getIsReadByUser();
    Boolean getIsHiddenByUser();
    Boolean getIsHiddenByAdmin();
}