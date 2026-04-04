package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface FullChatResult {
    Long getId();
    String getName();
    Boolean getIsGroup();
    Long getOpponentId();
    Integer getMembersCount();
    Integer getDeletedMembersCount();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
    MessageDBResult getLastMessage();
}