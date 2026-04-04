package com.sunrise.entity.dto;

import java.util.Map;

public record ChatMembersPageDTO(Map<Long, FullChatMemberDTO> chatMembers, Long nextCursor) { }