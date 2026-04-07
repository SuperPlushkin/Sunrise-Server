package com.sunrise.controller.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class AddGroupMembersRequest {
    @NotNull(message = "members is required")
    @NotEmpty(message = "members cannot be empty")
    @Size(max = 100, message = "Cannot add more than 100 members in one request")
    private Map<@NotNull(message = "User ID cannot be null")
                    @Min(value = 1, message = "User ID must be positive") Long,
                        @NotNull(message = "Admin flag cannot be null") Boolean> members;
}
