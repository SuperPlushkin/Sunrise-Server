package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface UserMessageDBResult {
    Long getId();
    Long getChatId();
    Long getSenderId();
    String getText();
    LocalDateTime getSentAt();
    Long getReadCount();
    Boolean getIsReadByUser();
    Boolean getIsDeleted();
}