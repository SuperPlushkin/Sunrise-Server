package com.Sunrise.DTOs.Paginations;

import java.time.LocalDateTime;

public interface UserFullChatResult {
    Long getId();
    String getName();
    Boolean getIsGroup();
    Long getOpponentId();
    Integer getMembersCount();
    Integer getDeletedMembersCount();
    UserMessageDBResult getLastMessage();
    Integer getMessagesCount();
    Integer getDeletedMessagesCount();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
}
