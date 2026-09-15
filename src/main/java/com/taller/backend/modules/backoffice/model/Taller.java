package com.taller.backend.modules.backoffice.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "talleres")
@Data
public class Taller {

    // El ID numérico auto-incremental (Lo que pide el repositorio)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El nombre del taller ya no lleva @Id
    private String nombre;

    private String titular;
    private String telefono;
    private String emailContacto;

    @Enumerated(EnumType.STRING)
    private EstadoSuscripcion estadoSuscripcion = EstadoSuscripcion.PRUEBA_GRATUITA;

    @Enumerated(EnumType.STRING)
    private TipoPlan tipoPlan = TipoPlan.BASE;

    private LocalDate fechaVencimiento;
}
