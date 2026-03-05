package com.Sunrise.DTO.DBResults;

import com.Sunrise.Entities.DTO.FullChatMemberDTO;

import java.util.Map;

public record ChatMembersPageResult(Map<Long, FullChatMemberDTO> chatMembers, Integer totalCount, Boolean hasMore) { }