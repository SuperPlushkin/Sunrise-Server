package com.Sunrise.DTO.DBResults;

import java.util.List;

public interface ChatMembersPageResult{
    List<Long> getUserIds();
    Integer getTotalCount();
    Boolean getHasMore();
}