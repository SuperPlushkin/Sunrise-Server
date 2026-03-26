package com.Sunrise.DTOs.Paginations;

public interface UserMessageDBResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getText();
    String getSentAt();
    Long getReadCount();
    Boolean getIsReadByUser();
    Boolean getIsHiddenByAdmin();
}