package com.sunrise.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class AddGroupMemberRequest {

    @NotNull(message = "newUserId cannot be null")
    @Min(value = 1, message = "newUserId must be at least 1")
    @Max(value = Long.MAX_VALUE, message = "newUserId must be at most " + Long.MAX_VALUE)
    private Long newUserId;
}
