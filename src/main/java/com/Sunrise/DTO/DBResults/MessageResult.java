package com.Sunrise.DTO.DBResults;

public interface MessageResult {
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