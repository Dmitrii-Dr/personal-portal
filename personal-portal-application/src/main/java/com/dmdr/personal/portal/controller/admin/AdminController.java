package com.dmdr.personal.portal.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Slf4j
public class AdminController {

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        // Stub JSON response
        Map<String, Object> dashboard = Map.of(
            "status", "success",
            "message", "Admin dashboard",
            "data", Map.of(
                "totalUsers", 0,
                "totalRoles", 0,
                "systemHealth", "operational",
                "lastUpdated", "2024-01-01T00:00:00Z"
            )
        );
        
        return ResponseEntity.ok(dashboard);
    }
}

