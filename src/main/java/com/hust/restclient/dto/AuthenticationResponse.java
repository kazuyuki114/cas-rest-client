package com.hust.restclient.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    private boolean success;
    private String message;
    private String serviceTicket;
    private String username;
    private String role;

    public static AuthenticationResponse success(String serviceTicket) {
        AuthenticationResponse response = new AuthenticationResponse();
        response.setSuccess(true);
        response.setMessage("Authenticate successful");
        response.setServiceTicket(serviceTicket);
        return response;
    }
    
    public static AuthenticationResponse success(String serviceTicket, String username, String role) {
        AuthenticationResponse response = new AuthenticationResponse();
        response.setSuccess(true);
        response.setMessage("Authenticate successful");
        response.setServiceTicket(serviceTicket);
        response.setUsername(username);
        response.setRole(role);
        return response;
    }

    public static AuthenticationResponse failure(String message) {
        AuthenticationResponse response = new AuthenticationResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
