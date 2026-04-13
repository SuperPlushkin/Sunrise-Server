package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.FullChatDTO;

import java.util.Map;

public record UserChatsPageDTO(Map<Long, FullChatDTO> chats, Long nextCursor) { }
