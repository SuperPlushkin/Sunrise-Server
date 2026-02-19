package com.Sunrise.DTO.DBResults;

import java.util.List;

public record ChatsPageResult(List<Long> chatIds, boolean hasMore, int totalCount) {}