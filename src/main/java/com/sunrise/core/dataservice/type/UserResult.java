package com.sunrise.core.dataservice.type;

import java.time.LocalDateTime;

public interface UserResult {
    Long getUserId();
    String getUsername();
    String getName();
    LocalDateTime getProfileUpdatedAt();
    LocalDateTime getCreatedAt();
    Boolean getIsEnabled();
    LocalDateTime getDeletedAt();
    Boolean getIsDeleted();
}