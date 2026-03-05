package com.Sunrise.DTO.DBResults;

import java.time.LocalDateTime;

public interface ChatMemberResult {
    Long getUserId();
    String getUsername();
    String getName();
    LocalDateTime getJoinedAt();
    Boolean getIsAdmin();
    Boolean getIsDeleted();
    Boolean getUserIsDeleted();
    Integer getTotalCount();
    Boolean getHasMore();
}
