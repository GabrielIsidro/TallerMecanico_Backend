package com.taller.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "planes_suscripcion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanSuscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoPlan tipo; // BASE o PRO

    @Enumerated(EnumType.STRING)
    private FrecuenciaPlan frecuencia; // MENSUAL o ANUAL

    private Double precio;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    private Boolean activo = true;
}
