package com.hust.restclient.dto;

public class CasLoginResult {
    private final boolean success;
    private final String message;
    private final String serviceTicket;
    private final String castgcCookie;
    private final CasUserDetail userDetail;

    private CasLoginResult(boolean success, String message, String serviceTicket, String castgcCookie, CasUserDetail userDetail) {
        this.success = success;
        this.message = message;
        this.serviceTicket = serviceTicket;
        this.castgcCookie = castgcCookie;
        this.userDetail = userDetail;
    }

    public static CasLoginResult success(String serviceTicket, String castgcCookie, CasUserDetail userDetail) {
        return new CasLoginResult(true, "Login successful", serviceTicket, castgcCookie, userDetail);
    }

    public static CasLoginResult failure(String message) {
        return new CasLoginResult(false, message, null, null, null);
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getServiceTicket() { return serviceTicket; }
    public String getCastgcCookie() { return castgcCookie; }
    public CasUserDetail getUserDetail() { return userDetail; }
}
