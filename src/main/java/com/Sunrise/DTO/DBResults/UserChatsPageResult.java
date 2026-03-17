package com.Sunrise.DTO.DBResults;

import com.Sunrise.Entities.DTO.FullChatDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record UserChatsPageResult(Map<Long, FullChatDTO> chats, int totalCount, boolean hasMore) {
    public Set<Long> getChatsId(){
        return new HashSet<>(chats.keySet());
    }
}
