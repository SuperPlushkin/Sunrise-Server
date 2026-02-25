package com.Sunrise.DTO.DBResults;

import java.util.List;

public interface ChatsPageResult{
    List<Long> getChatIds();
    Integer getTotalCount();
    Boolean getHasMore();
}