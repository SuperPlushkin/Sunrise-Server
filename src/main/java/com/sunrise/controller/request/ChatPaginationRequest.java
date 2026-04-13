package com.sunrise.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@lombok.Data
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class ChatPaginationRequest {
    Boolean isPinnedCursor;

    @Positive
    Long lastMsgIdCursor;

    @Positive
    Long chatIdCursor;

    @Min(10)
    @Max(100)
    private Integer limit;

    public Integer getLimit() {
        return limit != null ? limit : 20;
    }
}
