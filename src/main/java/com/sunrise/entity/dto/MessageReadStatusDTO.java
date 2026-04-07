package com.sunrise.entity.dto;

import java.time.LocalDateTime;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class MessageReadStatusDTO {
    private long userId;
    private LocalDateTime readAt;
}
