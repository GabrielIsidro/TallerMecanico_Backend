package com.taller.backend.core.dto;

public class AuthResponse {
    private final String jwt;
    private final String rol;
    private final String tallerId;

    public AuthResponse(String jwt, String rol, String tallerId) {
        this.jwt = jwt;
        this.rol = rol;
        this.tallerId = tallerId;
    }

    public String getJwt() { return jwt; }
    public String getRol() { return rol; }
    public String getTallerId() { return tallerId; }
}
