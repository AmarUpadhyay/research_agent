package com.researchagent.auth.dto;

public class AuthResponse {

    private final String token;
    private final String tokenType;

    public AuthResponse(String token) {
        this.token = token;
        this.tokenType = "Bearer";
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }
}
