package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface UserFullChatResult {
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

    Long getLastMessageId();
    Long getLastMessageChatId();
    Long getLastMessageSenderId();
    String getLastMessageText();
    LocalDateTime getLastMessageSentAt();
    Long getLastMessageReadCount();
    Boolean getLastMessageIsReadByUser();
    Boolean getLastMessageIsDeleted();
}
