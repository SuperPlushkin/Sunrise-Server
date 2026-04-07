package com.sunrise.entity.dto;

import java.util.Map;

public record MessagesPageDTO (Map<Long, MessageDTO> messages, Long nextCursor) {}