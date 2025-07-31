package com.hust.restclient.dto;

public class CasAuthenResult {
    private final boolean success;
    private final String message;
    private final String serviceTicket;

    private CasAuthenResult(boolean success, String message, String serviceTicket) {
        this.success = success;
        this.message = message;
        this.serviceTicket = serviceTicket;
    }

    public static CasAuthenResult success(String serviceTicket) {
        return new CasAuthenResult(true, "Authenticate successful", serviceTicket);
    }

    public static CasAuthenResult failure(String message) {
        return new CasAuthenResult(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getServiceTicket() { return serviceTicket; }
}
