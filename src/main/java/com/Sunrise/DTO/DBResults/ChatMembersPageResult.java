package com.Sunrise.DTO.DBResults;

import com.Sunrise.Entities.DTO.FullChatMemberDTO;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record ChatMembersPageResult(Map<Long, FullChatMemberDTO> chatMembers, int totalCount, boolean hasMore) {
    public Set<Long> getMembersId(){
        return new HashSet<>(chatMembers.keySet());
    }
}