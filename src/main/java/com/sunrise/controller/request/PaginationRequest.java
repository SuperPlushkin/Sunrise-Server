package com.sunrise.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {

    @Positive
    private Long cursor;

    @Min(10)
    @Max(100)
    private Integer limit;

    public Integer getLimit() {
        return limit != null ? limit : 20;
    }
}