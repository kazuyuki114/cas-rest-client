package com.hust.restclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserProfile(Authentication authentication) {
        log.info("User {} accessing profile", authentication.getName());
        return ResponseEntity.ok(Map.of(
            "message", "User profile",
            "username", authentication.getName(),
            "authorities", authentication.getAuthorities(),
            "profile", Map.of(
                "email", authentication.getName() + "@example.com",
                "status", "active"
            )
        ));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication authentication) {
        log.info("User {} accessing dashboard", authentication.getName());
        return ResponseEntity.ok(Map.of(
            "message", "Dashboard content",
            "user", authentication.getName(),
            "widgets", new String[]{"Recent Activity", "Notifications", "Quick Actions"}
        ));
    }
}
