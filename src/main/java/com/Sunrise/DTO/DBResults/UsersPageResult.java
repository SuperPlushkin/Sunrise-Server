package com.Sunrise.DTO.DBResults;

import java.util.List;

public record UsersPageResult(List<Long> userIds, boolean hasMore, int totalCount) {}
