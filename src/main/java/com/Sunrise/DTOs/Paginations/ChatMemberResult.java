package com.Sunrise.DTOs.Paginations;

import java.time.LocalDateTime;

public interface ChatMemberResult {
    Long getUserId();
    String getUsername();
    String getName();
    LocalDateTime getJoinedAt();
    Boolean getIsAdmin();
    Boolean getUserIsDeleted();
}
