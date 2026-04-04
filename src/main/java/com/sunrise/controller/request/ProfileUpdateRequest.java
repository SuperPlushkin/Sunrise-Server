package com.sunrise.controller.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Username must contain only letters, digits, and underscores"
    )
    private String username;

    @NotBlank(message = "Name is required")
    @Size(min = 4, max = 30, message = "Name must be between 4 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]+$",
        message = "Name must contain only letters, digits, and underscores"
    )
    private String name;
}