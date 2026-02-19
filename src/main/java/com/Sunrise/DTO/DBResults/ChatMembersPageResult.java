package com.Sunrise.DTO.DBResults;

import java.util.List;

public record ChatMembersPageResult(List<Long> userIds, int totalCount, boolean hasMore) {}