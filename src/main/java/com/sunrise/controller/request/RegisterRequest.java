package com.sunrise.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@lombok.Getter
@lombok.Setter
public class RegisterRequest {
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

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must contain at least one lowercase letter, one uppercase letter, one digit"
    )
    private String password;
}
