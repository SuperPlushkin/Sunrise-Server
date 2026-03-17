package com.Sunrise.DTO.DBResults;

public interface MessageDBResult {
    Long getMessageId();
    Long getChatId();
    Long getSenderId();
    String getText();
    String getSentAt();
    Long getReadCount();
    Long[] getReadByUsers();
    Boolean getIsHiddenByAdmin();
    Boolean getTooMany();
}