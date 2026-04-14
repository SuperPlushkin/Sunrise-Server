package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.UserChatDTO;

import java.util.Map;

public record UserChatsPageDTO(Map<Long, UserChatDTO> chats, Long nextCursor) { }
