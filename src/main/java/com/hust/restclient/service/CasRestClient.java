package com.hust.restclient.service;

import com.hust.restclient.config.CasConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CasRestClient {
    
    private final CasConfig casConfig;
    private final RestTemplate restTemplate;
    
    public CasConfig getCasConfig() {
        return casConfig;
    }
    
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
    
    /**
     * Step 1: Request TGT (Ticket Granting Ticket)
     */
    public String requestTgt(String username, String password) {
        String tgtUrl = casConfig.getServerUrl() + "v1/tickets";
        log.info("Requesting TGT from URL: {}", tgtUrl);
        log.info("CAS Server URL: {}", casConfig.getServerUrl());
        log.info("CAS Client Service URL: {}", casConfig.getClientServiceUrl());
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        // For REST protocol, we don't include service URL in TGT request
        // This avoids the SSO denial issue
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            log.info("Sending TGT request with body: {}", body);
            ResponseEntity<String> response = restTemplate.exchange(tgtUrl, HttpMethod.POST, request, String.class);
            
            log.info("TGT response status: {}", response.getStatusCode());
            log.info("TGT response headers: {}", response.getHeaders());
            log.info("TGT response body: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // Extract TGT from Location header
                String location = response.getHeaders().getFirst("Location");
                if (location != null && location.contains("TGT-")) {
                    String tgt = location.substring(location.lastIndexOf("/") + 1);
                    log.info("TGT obtained successfully: {}", tgt);
                    return tgt;
                } else {
                    log.error("TGT not found in Location header: {}", location);
                }
            }
            
            log.error("Failed to obtain TGT. Status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error requesting TGT", e);
            log.error("Error details: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
            }
            return null;
        }
    }
    
    /**
     * Step 2: Request ST (Service Ticket) using TGT
     */
    public String requestServiceTicket(String tgt, String service, String username, String password) {
        String stUrl = casConfig.getServerUrl() + "v1/tickets/" + tgt;
        log.info("Requesting Service Ticket from URL: {}", stUrl);
        log.info("Service URL being sent: {}", service);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("username", username);
        body.add("password", password);
        // Don't URL-encode the service URL in the request body
        body.add("service", service);
        log.info("ST request body: {}", body);
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        
        try {
            log.info("Sending ST request with headers: {}", headers);
            ResponseEntity<String> response = restTemplate.exchange(stUrl, HttpMethod.POST, request, String.class);
            
            log.info("ST response status: {}", response.getStatusCode());
            log.info("ST response headers: {}", response.getHeaders());
            log.info("ST response body: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String serviceTicket = response.getBody();
                log.info("Service ticket obtained successfully: {}", serviceTicket);
                return serviceTicket;
            }
            
            log.error("Failed to obtain service ticket. Status: {}", response.getStatusCode());
            return null;
            
        } catch (Exception e) {
            log.error("Error requesting service ticket", e);
            log.error("Error details: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Root cause: {}", e.getCause().getMessage());
            }
            return null;
        }
    }
    
    /**
     * Step 3: Validate Service Ticket
     */
    public boolean validateServiceTicket(String serviceTicket, String service) {
        String validateUrl = casConfig.getServerUrl() + "serviceValidate";
        // Don't URL-encode the service URL for validation
        String fullUrl = validateUrl + "?ticket=" + serviceTicket + "&service=" + service;
        log.info("Validating Service Ticket at URL: {}", fullUrl);
        
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(fullUrl, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                // Check if validation was successful (contains <cas:authenticationSuccess>)
                boolean isValid = responseBody != null && responseBody.contains("<cas:authenticationSuccess>");
                log.info("Service ticket validation result: {}", isValid);
                return isValid;
            }
            
            log.error("Failed to validate service ticket. Status: {}", response.getStatusCode());
            return false;
            
        } catch (Exception e) {
            log.error("Error validating service ticket", e);
            return false;
        }
    }
    
    /**
     * Complete CAS login flow
     */
    public CasLoginResult performCasLogin(String username, String password) {
        // Step 1: Request TGT
        String tgt = requestTgt(username, password);
        if (tgt == null) {
            return CasLoginResult.failure("Failed to obtain TGT");
        }
        
        // Step 2: Request ST
        String serviceTicket = requestServiceTicket(tgt, casConfig.getClientServiceUrl(), username, password);
        if (serviceTicket == null) {
            return CasLoginResult.failure("Failed to obtain service ticket");
        }
        
        // Step 3: Validate ST
        boolean isValid = validateServiceTicket(serviceTicket, casConfig.getClientServiceUrl());
        if (!isValid) {
            return CasLoginResult.failure("Service ticket validation failed");
        }
        
        // Generate CASTGC cookie value (this would typically be set by the browser)
        String castgcCookie = "CASTGC=" + tgt + "; Path=/; Secure; HttpOnly";
        
        return CasLoginResult.success(serviceTicket, castgcCookie);
    }
    
    public static class CasLoginResult {
        private final boolean success;
        private final String message;
        private final String serviceTicket;
        private final String castgcCookie;
        
        private CasLoginResult(boolean success, String message, String serviceTicket, String castgcCookie) {
            this.success = success;
            this.message = message;
            this.serviceTicket = serviceTicket;
            this.castgcCookie = castgcCookie;
        }
        
        public static CasLoginResult success(String serviceTicket, String castgcCookie) {
            return new CasLoginResult(true, "Login successful", serviceTicket, castgcCookie);
        }
        
        public static CasLoginResult failure(String message) {
            return new CasLoginResult(false, message, null, null);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getServiceTicket() { return serviceTicket; }
        public String getCastgcCookie() { return castgcCookie; }
    }
} 