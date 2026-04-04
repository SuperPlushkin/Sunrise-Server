package com.sunrise.entity.dto;

import java.util.Map;

public record MessagesPageDTO (Map<Long, LightMessageDTO> messages, Long nextCursor) {}