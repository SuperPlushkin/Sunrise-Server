package com.Sunrise.DTO.DBResults;

import java.util.List;

public interface UsersPageResult {
    List<Long> getUserIds();
    Integer getTotalCount();
    Boolean getHasMore();
}
