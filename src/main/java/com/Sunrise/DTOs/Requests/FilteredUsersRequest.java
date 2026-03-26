package com.Sunrise.DTOs.Requests;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@lombok.Getter
@lombok.Setter
public class FilteredUsersRequest {

    @JsonSetter(nulls = Nulls.SKIP)
    private String filter = "";

    @Min(value = 1, message = "limited must be at least 1")
    @Max(value = 50, message = "limited must be at most 50")
    @JsonSetter(nulls = Nulls.SKIP)
    private Integer limit = 1;
}
