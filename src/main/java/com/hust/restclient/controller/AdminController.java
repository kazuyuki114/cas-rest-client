package com.hust.restclient.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers(Authentication authentication) {
        log.info("Admin {} accessing user list", authentication.getName());
        return ResponseEntity.ok(Map.of(
            "message", "Admin-only content: User list",
            "user", authentication.getName(),
            "authorities", authentication.getAuthorities(),
            "data", "This is sensitive admin data"
        ));
    }

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getReports(Authentication authentication) {
        log.info("Admin {} accessing reports", authentication.getName());
        return ResponseEntity.ok(Map.of(
            "message", "Admin reports",
            "user", authentication.getName(),
            "reports", new String[]{"Financial Report", "User Activity", "System Health"}
        ));
    }
}
