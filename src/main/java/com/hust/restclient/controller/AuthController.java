package com.hust.restclient.controller;

import com.hust.restclient.dto.LoginRequest;
import com.hust.restclient.dto.LoginResponse;
import com.hust.restclient.service.CasRestClient;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final CasRestClient casRestClient;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, 
                                             HttpServletResponse response) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        
        try {
            CasRestClient.CasLoginResult result = casRestClient.performCasLogin(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            if (result.isSuccess()) {
                // Set CASTGC cookie in response
                response.setHeader("Set-Cookie", result.getCastgcCookie());
                
                LoginResponse loginResponse = LoginResponse.success(
                    result.getServiceTicket(), 
                    result.getCastgcCookie()
                );
                
                log.info("Login successful for user: {}", loginRequest.getUsername());
                return ResponseEntity.ok(loginResponse);
                
            } else {
                log.warn("Login failed for user: {}. Reason: {}", 
                    loginRequest.getUsername(), result.getMessage());
                
                LoginResponse loginResponse = LoginResponse.failure(result.getMessage());
                return ResponseEntity.badRequest().body(loginResponse);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
            LoginResponse loginResponse = LoginResponse.failure("Internal server error");
            return ResponseEntity.internalServerError().body(loginResponse);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("CAS Client is running");
    }
    
    @GetMapping("/config")
    public ResponseEntity<String> config() {
        return ResponseEntity.ok("CAS Server URL: " + casRestClient.getCasConfig().getServerUrl() + 
                               ", CAS Client Service URL: " + casRestClient.getCasConfig().getClientServiceUrl());
    }
    
    @GetMapping("/test-ssl")
    public ResponseEntity<String> testSsl() {
        try {
            String testUrl = casRestClient.getCasConfig().getServerUrl() + "login";
            log.info("Testing SSL connection to: {}", testUrl);
            
            // Simple GET request to test SSL connection
            ResponseEntity<String> response = casRestClient.getRestTemplate().getForEntity(testUrl, String.class);
            log.info("SSL test successful. Status: {}", response.getStatusCode());
            return ResponseEntity.ok("SSL connection successful. Status: " + response.getStatusCode());
            
        } catch (Exception e) {
            log.error("SSL test failed", e);
            return ResponseEntity.status(500).body("SSL connection failed: " + e.getMessage());
        }
    }
} 