package com.Sunrise.Controllers;

import com.Sunrise.Services.DataServices.DataAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthTestController {

    @Autowired
    private DataAccessService dataAccessService;

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
            "name", "Sunrise-Server",
            "status", "🟢 Онлайн",
            "version", "0.2",
            "users", "skoro"
        );
    }

    @GetMapping("/cash-status")
    public ResponseEntity<?> getCashStatus() {
        return ResponseEntity.ok(dataAccessService.getCacheStatus());
    }
}
