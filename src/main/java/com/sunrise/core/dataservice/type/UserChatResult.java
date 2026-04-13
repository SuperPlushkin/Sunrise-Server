package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface UserChatResult {
    Long getId();
    String getName();
    String getDescription();
    String getChatType();
    Long getOpponentId();
    Integer getMembersCount();
    Integer getDeletedMembersCount();
    LocalDateTime getUpdatedAt();
    LocalDateTime getCreatedAt();
    Long getCreatedBy();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
    Boolean getIsPinned();
    Integer getUnreadMessagesCount();

    Long getLastMessageId();
    Long getLastMessageChatId();
    Long getLastMessageSenderId();
    String getLastMessageText();
    Long getLastMessageReadCount();
    Boolean getLastMessageIsReadByUser();
    LocalDateTime getLastMessageSentAt();
    LocalDateTime getLastMessageUpdatedAt();
    LocalDateTime getLastMessageDeletedAt();
    Boolean getLastMessageIsDeleted();
}
