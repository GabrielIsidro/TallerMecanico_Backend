package com.taller.backend.modules.backoffice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TallerRegistroDTO {

    @NotBlank(message = "El nombre del taller es obligatorio")
    private String nombre;

    @NotBlank(message = "El nombre del titular es obligatorio")
    private String titular;

    private String telefono;

    @NotBlank(message = "El email de contacto es obligatorio")
    @Email(message = "El formato del email de contacto no es válido")
    private String emailContacto;

    // Contraseña inicial opcional: si no se provee, se autogenera una clave temporal de 10 caracteres
    private String password;
    
    // Nombres del usuario admin para mayor personalización
    private String nombreAdmin;
    private String apellidoAdmin;
}
