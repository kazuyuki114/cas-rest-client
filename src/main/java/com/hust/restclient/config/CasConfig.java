package com.hust.restclient.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
@Configuration
public class CasConfig {
    
    @Value("${cas.server.url}")
    private String serverUrl;
    
    @Value("${cas.client.service.url}")
    private String clientServiceUrl;
    
    @PostConstruct
    public void validateConfiguration() {
        log.info("CAS Configuration loaded:");
        log.info("Server URL: {}", serverUrl);
        log.info("Client Service URL: {}", clientServiceUrl);
        
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalStateException("CAS server URL is not configured");
        }
        
        if (clientServiceUrl == null || clientServiceUrl.trim().isEmpty()) {
            throw new IllegalStateException("CAS client service URL is not configured");
        }
        
        // Ensure server URL ends with / if it doesn't already
        if (!serverUrl.endsWith("/")) {
            serverUrl = serverUrl + "/";
        }
    }
} 