package com.sunrise.entity.pagination;

import com.sunrise.entity.dto.MessageDTO;

import java.util.Map;

public record MessagesPageDTO (Map<Long, MessageDTO> messages, Long nextCursor) {}