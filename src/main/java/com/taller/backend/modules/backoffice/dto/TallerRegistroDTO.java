package com.taller.backend.modules.backoffice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TallerRegistroDTO {
    private String nombre;
    private String titular;
    private String telefono;
    private String emailContacto;
    private String password;
    
    // Nombres del usuario admin para mayor personalización
    private String nombreAdmin;
    private String apellidoAdmin;
}
