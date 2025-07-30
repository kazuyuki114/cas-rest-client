package com.hust.restclient.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private boolean success;
    private String message;
    private String serviceTicket;
    private String castgcCookie;
    
    public static LoginResponse success(String serviceTicket, String castgcCookie) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(true);
        response.setMessage("Login successful");
        response.setServiceTicket(serviceTicket);
        response.setCastgcCookie(castgcCookie);
        return response;
    }
    
    public static LoginResponse failure(String message) {
        LoginResponse response = new LoginResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
} 