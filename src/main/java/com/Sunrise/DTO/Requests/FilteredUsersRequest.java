package com.Sunrise.DTO.Requests;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@lombok.Getter
@lombok.Setter
public class FilteredUsersRequest {

    @Min(value = 1, message = "limited must be at least 1")
    @Max(value = 50, message = "limited must be at most 50")
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer limited = 1;

    @Min(value = 0, message = "offset must be at least 0")
    @Max(value = Integer.MAX_VALUE, message = "limited must be at most 50")
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer offset = 0;

    @JsonSetter(nulls = Nulls.SKIP)
    private String filter = "";
}
