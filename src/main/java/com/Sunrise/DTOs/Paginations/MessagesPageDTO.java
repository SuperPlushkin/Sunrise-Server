package com.Sunrise.DTOs.Paginations;

import com.Sunrise.Entities.DTOs.LightMessageDTO;

import java.util.Map;

public record MessagesPageDTO (Map<Long, LightMessageDTO> messages, Long nextCursor) {}