package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.ChatMemberProfileDTO;

import java.util.Map;

public record ChatMembersPageDTO(Map<Long, ChatMemberProfileDTO> chatMembers, Long nextCursor) { }