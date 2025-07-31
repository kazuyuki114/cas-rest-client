package com.hust.restclient.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hust.restclient.dto.AuthenticationResponse;
import com.hust.restclient.dto.CasLoginResult;
import com.hust.restclient.dto.CasUserDetail;
import com.hust.restclient.dto.LoginRequest;
import com.hust.restclient.dto.LoginResponse;
import com.hust.restclient.service.CasRestClient;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final CasRestClient casRestClient;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest, 
                                             HttpServletResponse response,
                                             HttpServletRequest request) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());
        
        try {
            CasLoginResult result = casRestClient.performCasLogin(
                loginRequest.getUsername(), 
                loginRequest.getPassword()
            );
            
            if (result.isSuccess()) {
                // Set CASTGC cookie in response
                response.setHeader("Set-Cookie", result.getCastgcCookie());
                
                // Get user details from login result (already validated)
                CasUserDetail userDetail = result.getUserDetail();
                String actualRole = "USER"; // Default role
                if (userDetail != null && userDetail.getRole() != null) {
                    actualRole = userDetail.getRole();
                }
                
                // Store authentication in HTTP session with CORRECT role
                HttpSession session = request.getSession(true);
                session.setAttribute("authenticated_username", loginRequest.getUsername());
                session.setAttribute("user_role", actualRole); // Use actual role from CAS
                session.setAttribute("cas_tgt", extractTgtFromCookie(result.getCastgcCookie()));
                session.setMaxInactiveInterval(30 * 60); // 30 minutes
                
                log.info("User {} logged in successfully with role: {}, session created: {}", 
                        loginRequest.getUsername(), actualRole, session.getId());
                
                LoginResponse loginResponse = LoginResponse.success(
                    result.getServiceTicket()
                );
                return ResponseEntity.ok(loginResponse);
                
            } else {
                LoginResponse loginResponse = LoginResponse.failure(result.getMessage());
                return ResponseEntity.badRequest().body(loginResponse);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
            LoginResponse loginResponse = LoginResponse.failure("Internal server error");
            return ResponseEntity.internalServerError().body(loginResponse);
        }
    }
    
    private String extractTgtFromCookie(String castgcCookie) {
        // Extract TGT from "CASTGC=TGT-123-abc; Path=/; Secure; HttpOnly"
        if (castgcCookie != null && castgcCookie.startsWith("CASTGC=")) {
            String value = castgcCookie.substring(7); // Remove "CASTGC="
            int semicolonIndex = value.indexOf(';');
            return semicolonIndex > 0 ? value.substring(0, semicolonIndex) : value;
        }
        return null;
    }

    @PostMapping("authen")
    public ResponseEntity<AuthenticationResponse> authenticate(HttpServletRequest request){
        try{
            Cookie[] cookies = request.getCookies();
            String castgc = null;
            if (cookies != null) {
                castgc = Arrays.stream(cookies)
                        .filter(c -> "CASTGC".equals(c.getName()))
                        .map(Cookie::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (castgc == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Get service ticket using TGT
            String serviceTicket = casRestClient.requestServiceTicket(
                castgc, 
                casRestClient.getCasConfig().getClientServiceUrl(),
                null, 
                null
            );
            
            if (serviceTicket == null) {
                AuthenticationResponse authenResponse = AuthenticationResponse.failure("Invalid session");
                return ResponseEntity.badRequest().body(authenResponse);
            }
            
            // Validate service ticket and get user details
            CasUserDetail userDetail = casRestClient.validateServiceTicket(
                serviceTicket, 
                casRestClient.getCasConfig().getClientServiceUrl()
            );
            
            if (userDetail.isSuccess()) {
                log.info("User authenticated: {} with role: {}", userDetail.getUsername(), userDetail.getRole());
                return ResponseEntity.ok(AuthenticationResponse.success(
                    serviceTicket, 
                    userDetail.getUsername(), 
                    userDetail.getRole()
                ));
            } else {
                AuthenticationResponse authenResponse = AuthenticationResponse.failure("Authentication failed");
                return ResponseEntity.badRequest().body(authenResponse);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error during authenticating the user", e);
            AuthenticationResponse authenResponse = AuthenticationResponse.failure("Internal server error");
            return ResponseEntity.internalServerError().body(authenResponse);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Invalidate HTTP session
            HttpSession session = request.getSession(false);
            if (session != null) {
                String username = (String) session.getAttribute("authenticated_username");
                session.invalidate();
                log.info("Session invalidated for user: {}", username);
            }
            
            // Clear CASTGC cookie
            Cookie castgcCookie = new Cookie("CASTGC", "");
            castgcCookie.setMaxAge(0);
            castgcCookie.setPath("/");
            castgcCookie.setHttpOnly(true);
            response.addCookie(castgcCookie);
            
            // Clear JSESSIONID cookie
            Cookie jsessionCookie = new Cookie("JSESSIONID", "");
            jsessionCookie.setMaxAge(0);
            jsessionCookie.setPath("/");
            jsessionCookie.setHttpOnly(true);
            response.addCookie(jsessionCookie);
            
            return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "action", "redirect_to_login"
            ));
            
        } catch (Exception e) {
            log.error("Error during logout", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "Logout failed",
                "message", "Internal server error"
            ));
        }
    }
} 