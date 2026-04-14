package com.sunrise.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UpdateChatInfoRequest {
    @NotBlank(message = "chatName is required")
    @Size(min = 4, max = 30, message = "chatName must be between 4 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "chatName must contain only letters, digits, and underscores"
    )
    private String chatName;

    @Size(max = 500, message = "chatDescription mustn`t be more than 500 characters")
    private String chatDescription;
}
