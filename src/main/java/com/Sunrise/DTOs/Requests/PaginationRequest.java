package com.Sunrise.DTOs.Requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
public class PaginationRequest {
    @Positive
    private Long cursor = null;

    @Min(10)
    @Max(100)
    private int limit = 20;
}
