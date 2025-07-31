package com.hust.restclient.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ValidateRequest {
    @NotBlank(message = "Service ticket is required")
    private String serviceTicket;
    
    @NotBlank(message = "Service URL is required")
    private String service;
}
