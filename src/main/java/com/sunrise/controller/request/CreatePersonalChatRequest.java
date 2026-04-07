package com.sunrise.controller.request;

import com.sunrise.config.annotation.ValidId;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class CreatePersonalChatRequest {

    @ValidId
    private Long otherUserId;
}