package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface UserMessageDBResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getText();
    Long getReadCount();
    Boolean getIsReadByUser();
    LocalDateTime getSentAt();
    LocalDateTime getUpdatedAt();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
}