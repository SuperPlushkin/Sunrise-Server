package com.sunrise.controller;

import com.sunrise.controller.request.LoginRequest;
import com.sunrise.controller.request.RegisterRequest;
import com.sunrise.core.service.result.TokenConfirmationResult;
import com.sunrise.core.service.result.UserLoginResult;
import com.sunrise.core.service.result.UserRegistrationResult;
import com.sunrise.core.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @ResponseBody
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {

        UserRegistrationResult result = authService.registerUser(
            request.getUsername().trim(),
            request.getName().trim(),
            request.getEmail().trim(),
            request.getPassword().trim()
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok("User registered successfully. Check your mail to activate your account!!!");
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @ResponseBody
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {

        UserLoginResult result = authService.authenticateUser(
            request.getUsername().trim(),
            request.getPassword().trim(),
            httpRequest
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(Map.of(
                "token", result.getJwtToken(),
                "expiration", result.getExpiration()
            ));
        }
        else {
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/confirm")
    public String confirmEmail(@RequestParam @NotBlank String type,
                               @RequestParam @Size(min = 64, max = 64) String token,
                               Model model) {

        TokenConfirmationResult result = authService.confirmToken(type, token);

        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getOperationText());
        model.addAttribute("type", type);

        return "email-confirmation";
    }

    @GetMapping("/test-html")
    public String testHtml(Model model) {
        model.addAttribute("isSuccess", true);
        model.addAttribute("message", "Тестовая страница работает!");
        return "email-confirmation";
    }
}