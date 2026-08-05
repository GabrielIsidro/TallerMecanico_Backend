package com.taller.backend.dto;

import lombok.Data;

@Data
public class ActualizarPerfilRequest {
    // Estos dos campos son nuevos
    private String nombre;
    private String apellido;

    // Estos dos se quedan
    private String passwordActual;
    private String passwordNueva;
}