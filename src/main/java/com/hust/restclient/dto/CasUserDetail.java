package com.hust.restclient.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CasUserDetail {
    private boolean success;
    private String username;
    private String role;
    
    public static CasUserDetail success(String username, String role) {
        return new CasUserDetail(true, username, role);
    }
    
    public static CasUserDetail failure() {
        return new CasUserDetail(false, null, null);
    }
}