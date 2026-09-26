package com.taller.backend.core.dto;

import lombok.Data;

@Data
public class RegistroRequestDTO {
    private String email;
    private String password;
    private String nombre;
    private String apellido;
}
