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
    private Boolean debeCambiarPassword = false;

    public UsuarioResponseDTO(Long id, String email, String nombre, String apellido, RolUsuario rol,
                              Long tallerId, String tallerNombre, EstadoSuscripcion estadoSuscripcion,
                              com.taller.backend.modules.backoffice.model.TipoPlan tipoPlan, LocalDate fechaVencimiento) {
        this.id = id;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.tallerId = tallerId;
        this.tallerNombre = tallerNombre;
        this.estadoSuscripcion = estadoSuscripcion;
        this.tipoPlan = tipoPlan;
        this.fechaVencimiento = fechaVencimiento;
        this.debeCambiarPassword = false;
    }
}
