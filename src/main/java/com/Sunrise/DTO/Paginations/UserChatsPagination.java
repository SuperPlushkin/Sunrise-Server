package com.Sunrise.DTO.Paginations;

import java.time.LocalDateTime;
import java.util.Set;

@lombok.Builder
public record UserChatsPagination(long id, long userId, int offset, int limit, Set<Long> chatIds, LocalDateTime createdAt, boolean hasMore, int totalCount) { }