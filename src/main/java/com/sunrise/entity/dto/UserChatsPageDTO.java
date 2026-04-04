package com.sunrise.entity.dto;

import java.util.Map;

public record UserChatsPageDTO(Map<Long, FullChatDTO> chats, Long nextCursor) { }
