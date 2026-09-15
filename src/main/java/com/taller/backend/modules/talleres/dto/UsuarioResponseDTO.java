package com.taller.backend.modules.talleres.dto;

import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponseDTO {
    private Long id;
    private String email;
    private String nombre;
    private String apellido;
    private RolUsuario rol;
    private Long tallerId;
    private String tallerNombre;
    private EstadoSuscripcion estadoSuscripcion;
    private com.taller.backend.modules.backoffice.model.TipoPlan tipoPlan;
    private LocalDate fechaVencimiento;
}
