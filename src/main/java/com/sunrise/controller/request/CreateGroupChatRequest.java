package com.sunrise.controller.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.*;

import java.util.Map;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CreateGroupChatRequest {

    @NotBlank(message = "chatName is required")
    @Size(min = 4, max = 30, message = "chatName must be between 4 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "chatName must contain only letters, digits, and underscores"
    )
    private String chatName;

    @NotNull(message = "members is required")
    @Size(max = 100, message = "Group cannot have more than 100 members")
    private Map<@NotNull(message = "User ID cannot be null")
                    @Min(value = 1, message = "User ID must be positive") Long,
                        @NotNull(message = "Admin flag cannot be null") Boolean> members;
}
