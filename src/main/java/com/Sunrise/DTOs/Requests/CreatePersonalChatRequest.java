package com.Sunrise.DTOs.Requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CreatePersonalChatRequest {

    @NotNull(message = "otherUserId cannot be null")
    @Min(value = 1, message = "otherUserId must be at least 1")
    @Max(value = Long.MAX_VALUE, message = "otherUserId must be at most " + Long.MAX_VALUE)
    private Long otherUserId;
}