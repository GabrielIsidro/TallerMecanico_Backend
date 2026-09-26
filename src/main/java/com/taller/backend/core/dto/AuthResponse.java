package com.taller.backend.core.dto;

public class AuthResponse {
    private final String jwt;
    private final String rol;
    private final String tallerId;
    private final Boolean debeCambiarPassword;
    private final String estadoSuscripcion;

    public AuthResponse(String jwt, String rol, String tallerId) {
        this(jwt, rol, tallerId, false, null);
    }

    public AuthResponse(String jwt, String rol, String tallerId, Boolean debeCambiarPassword) {
        this(jwt, rol, tallerId, debeCambiarPassword, null);
    }

    public AuthResponse(String jwt, String rol, String tallerId, Boolean debeCambiarPassword, String estadoSuscripcion) {
        this.jwt = jwt;
        this.rol = rol;
        this.tallerId = tallerId;
        this.debeCambiarPassword = debeCambiarPassword != null ? debeCambiarPassword : false;
        this.estadoSuscripcion = estadoSuscripcion;
    }

    public String getJwt() { return jwt; }
    public String getRol() { return rol; }
    public String getTallerId() { return tallerId; }
    public Boolean getDebeCambiarPassword() { return debeCambiarPassword; }
    public String getEstadoSuscripcion() { return estadoSuscripcion; }
}
