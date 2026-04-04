package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface UserFullChatResult {
    Long getId();
    String getName();
    Boolean getIsGroup();
    Long getOpponentId();
    Integer getMembersCount();
    Integer getDeletedMembersCount();
    UserMessageDBResult getLastMessage();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
}
