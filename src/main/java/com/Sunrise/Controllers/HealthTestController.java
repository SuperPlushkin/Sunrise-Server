package com.Sunrise.Controllers;

import com.Sunrise.Core.DataServices.DataOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthTestController {

    @Autowired
    private DataOrchestrator dataOrchestrator;

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
            "name", "Sunrise-Server",
            "status", "🟢 Онлайн",
            "version", "0.2",
            "users", "skoro"
        );
    }

    @GetMapping("/cache-status")
    public ResponseEntity<?> getCashStatus() {
        return ResponseEntity.ok(dataOrchestrator.getCacheStatus());
    }
}
