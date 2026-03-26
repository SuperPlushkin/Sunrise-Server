package com.Sunrise.DTOs.Requests;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.*;

import java.util.HashSet;
import java.util.Set;

@lombok.Getter
@lombok.Setter
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

    @Size(max = 100, message = "Group cannot have more than 100 members")
    @JsonSetter(nulls = Nulls.SKIP)
    private Set<@NotNull(message = "UserId cannot be null") @Min(1) Long> userIds = new HashSet<>();
}
