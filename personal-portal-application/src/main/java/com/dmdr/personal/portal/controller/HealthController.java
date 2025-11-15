package com.dmdr.personal.portal.controller;

import com.dmdr.personal.portal.service.JwtService;
import com.dmdr.personal.portal.users.dto.AccessLevelResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/health")
@Slf4j
public class HealthController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public HealthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/access")
    public ResponseEntity<AccessLevelResponse> checkAccess(HttpServletRequest request) {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        // No token provided - public access
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            AccessLevelResponse response = new AccessLevelResponse();
            response.setAccessLevel("PUBLIC");
            response.setMessage("No authentication token provided. Public access only.");
            return ResponseEntity.ok(response);
        }

        // Extract token
        String token = authHeader.substring(BEARER_PREFIX.length());

        // Validate token
        if (!jwtService.isValidToken(token)) {
            AccessLevelResponse response = new AccessLevelResponse();
            response.setAccessLevel("PUBLIC");
            response.setMessage("Invalid or expired token. Public access only.");
            return ResponseEntity.ok(response);
        }

        // Extract user info and roles
        String email = jwtService.extractEmail(token);
        Set<String> roles = jwtService.extractRoles(token);

        AccessLevelResponse response = new AccessLevelResponse();
        response.setEmail(email);

        // Determine access level based on roles
        if (roles.contains(ROLE_ADMIN)) {
            response.setAccessLevel("ADMIN");
            response.setMessage("Admin access granted. Full system access.");
        } else if (roles.contains(ROLE_USER)) {
            response.setAccessLevel("USER");
            response.setMessage("User access granted. Standard user privileges.");
        } else {
            response.setAccessLevel("PUBLIC");
            response.setMessage("No recognized roles found. Public access only.");
        }

        return ResponseEntity.ok(response);
    }
}

