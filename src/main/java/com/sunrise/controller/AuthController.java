package com.sunrise.controller;

import com.sunrise.controller.request.LoginRequest;
import com.sunrise.controller.request.RegisterRequest;
import com.sunrise.core.service.result.*;
import com.sunrise.core.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;
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

    @GetMapping("/confirm")
    public String confirmEmail(@RequestParam @NotBlank String type,
                               @RequestParam @Size(min = 64, max = 64) String token,
                               Model model) {

        ResultOneArg<String> result = authService.confirmToken(type, token);

        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getResult());
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