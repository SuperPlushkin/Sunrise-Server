package com.sunrise.controller;

import com.sunrise.config.annotation.CurrentUserId;
import com.sunrise.controller.request.LoginRequest;
import com.sunrise.controller.request.RegisterRequest;
import com.sunrise.core.service.result.*;
import com.sunrise.core.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@Controller // Не менять, потому что html работать не будет
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @ResponseBody
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {

        ResultOneArg<String> result = authService.registerUser(
            request.getUsername().trim(),
            request.getName().trim(),
            request.getEmail().trim(),
            request.getPassword().trim()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @ResponseBody
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {

        ResultOneArg<UserLoginResult> result = authService.authenticateUser(
            request.getUsername().trim(),
            request.getPassword().trim(),
            httpRequest
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getResult());
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @ResponseBody
    @PostMapping("/change-email")
    public ResponseEntity<?> requestEmailChange(@RequestParam @NotBlank @Email String newEmail, @CurrentUserId long userId) {
        ResultNoArgs result = authService.requestEmailUpdate(userId, newEmail);
        if (result.isSuccess()) {
            return ResponseEntity.ok("Confirmation sent to your current email address");
        } else {
            return ResponseEntity.badRequest().body(result.getError());
        }
    }

    @ResponseBody
    @PostMapping("/reset-password")
    public ResponseEntity<?> requestPasswordUpdate(@RequestParam
                                                   @NotBlank(message = "Username is required")
                                                   @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
                                                   @Pattern(
                                                           regexp = "^[a-zA-Z0-9_]+$",
                                                           message = "Username must contain only letters, digits, and underscores"
                                                   ) String username) {
        ResultNoArgs result = authService.requestPasswordUpdate(username);
        return ResponseEntity.ok("If the email exists, a reset link has been sent");
    }

    @GetMapping("/confirm-registration")
    public String confirmRegistration(@RequestParam @Size(min = 64, max = 64) String token, Model model) {

        ResultOneArg<String> result = authService.confirmRegistrationToken(token);

        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());

        return "confirm-registration";
    }

    @GetMapping("/confirm-email-update")
    public String showEmailUpdateForm(@RequestParam @Size(min = 64, max = 64) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("submitted", false);
        return "confirm-email-update";
    }
    @PostMapping("/confirm-email-update")
    public String confirmEmailUpdate(@RequestParam @Size(min = 64, max = 64) String token,
                                     @RequestParam @NotBlank @Email String email,
                                     Model model) {
        ResultOneArg<String> result = authService.confirmEmailUpdateToken(token, email);
        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());
        model.addAttribute("submitted", true);
        return "confirm-email-update";
    }

    @GetMapping("/confirm-password-update")
    public String showPasswordUpdateForm(@RequestParam @Size(min = 64, max = 64) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("submitted", false);
        return "confirm-password-update";
    }
    @PostMapping("/confirm-password-update")
    public String confirmPasswordUpdate(@RequestParam @Size(min = 64, max = 64) String token,
                                        @RequestParam @NotBlank @Size(min = 8, max = 30) String password,
                                        Model model) {
        ResultOneArg<String> result = authService.confirmPasswordUpdateToken(token, password);
        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());
        model.addAttribute("submitted", true);
        return "confirm-password-update";
    }
}