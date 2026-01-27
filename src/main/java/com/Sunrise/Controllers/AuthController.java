package com.Sunrise.Controllers;

import com.Sunrise.DTO.Requests.LoginRequest;
import com.Sunrise.DTO.Requests.RegisterRequest;
import com.Sunrise.Services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService = authService;
    }

    @ResponseBody
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {

        String username = request.getUsername().trim();

        var result = authService.registerUser(username, request.getName().trim(), request.getEmail().trim(), request.getPassword().trim());

        if (result.isSuccess()) {
            log.info("User registered successfully --> {}", username);
            return ResponseEntity.ok("User registered successfully. Check your mail to activate your account!!!");
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @ResponseBody
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        String username = request.getUsername().trim();
        String password = request.getPassword().trim();

        var result = authService.authenticateUser(username, password, httpRequest);

        if (result.isSuccess()) {
            log.info("User login successfully --> {}", username);
            return ResponseEntity.ok(Map.of(
                "token", result.getJwtToken(),
                "expiration", result.getExpiration()
            ));
        }
        else {
            log.warn(result.getErrorMessage());
            return ResponseEntity.badRequest().body(result.getErrorMessage());
        }
    }

    @GetMapping("/confirm")
    public String confirmEmail(@RequestParam("type") String type, @RequestParam("token") @Size(min = 64, max = 64) String token, Model model) {

        var result = authService.confirmToken(type, token);

        model.addAttribute("isSuccess", result.isSuccess());
        model.addAttribute("message", result.getOperationText());
        model.addAttribute("type", type);

        return "email-confirmation";
    }

    @GetMapping("/test-html")
    public String testHtml(Model model) {
        log.info("Test HTML page accessed");
        model.addAttribute("isSuccess", true);
        model.addAttribute("message", "Тестовая страница работает!");
        return "email-confirmation";
    }
}