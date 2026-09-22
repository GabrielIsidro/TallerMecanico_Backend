package com.taller.backend.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email no es válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
