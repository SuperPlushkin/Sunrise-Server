package com.Sunrise.DTOs.Paginations;

import com.Sunrise.Entities.DTOs.FullChatMemberDTO;

import java.util.Map;

public record ChatMembersPageDTO(Map<Long, FullChatMemberDTO> chatMembers, Long nextCursor) { }