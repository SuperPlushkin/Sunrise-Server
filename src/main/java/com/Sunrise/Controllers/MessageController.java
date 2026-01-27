package com.Sunrise.Controllers;

import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/actions")
public class MessageController {

    private List<String> messages = new ArrayList<>();

    @GetMapping("/hello")
    public String hello() {
        return "Hello, this is protected endpoint!";
    }

    @GetMapping("/messages")
    public List<String> getMessages() {
        return messages;
    }

    @PostMapping("/messages")
    public String addMessage(@RequestBody String message) {
        messages.add(message);
        return "Сообщение добавлено: " + message;
    }

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
            "status", "🟢 Онлайн",
            "version", "1.0",
            "users", "1"
        );
    }
}
