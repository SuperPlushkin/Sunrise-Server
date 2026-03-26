package com.Sunrise.DTOs.Paginations;

import java.util.Set;

@lombok.Getter
public final class UserChatsPagination extends PaginationTemplate {
    private final long userId;

    public UserChatsPagination(long id, long userId, Long cursor, int limit) {
        super(id, cursor, limit);
        this.userId = userId;
    }
}