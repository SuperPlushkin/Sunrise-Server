package com.Sunrise.DTO.Paginations;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Set;

@Builder
public record UsersPagination(long id, String filter, int offset, int limit, Set<Long> userIds, LocalDateTime createdAt, boolean hasMore, int totalCount) { }
