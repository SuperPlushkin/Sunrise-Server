package com.Sunrise.DTOs.DBResults;

import java.time.LocalDateTime;

public interface FullChatResult {
    Long getId();
    String getName();
    Boolean getIsGroup();
    Long getOpponentId();
    Integer getMembersCount();
    Integer getDeletedMembersCount();
    Integer getMessagesCount();
    Integer getDeletedMessagesCount();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
    MessageDBResult getLastMessage();
}
