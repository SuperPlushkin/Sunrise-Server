package com.Sunrise.DTOs.Paginations;

import com.Sunrise.Entities.DTOs.FullChatDTO;

import java.util.Map;

public record UserChatsPageDTO(Map<Long, FullChatDTO> chats, Long nextCursor) { }
