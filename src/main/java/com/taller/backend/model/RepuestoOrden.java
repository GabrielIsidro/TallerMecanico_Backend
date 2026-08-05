package com.taller.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "repuestos_orden")
@Data
public class RepuestoOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion; // Ej: "Juego de pastillas de freno Bosch"
    private String proveedor;   // Ej: "Repuestos Carlitos" (Le sirve al mecánico para recordar)
    
    private Double costo;         // Lo que le costó al taller (para calcular ganancia a futuro)
    private Double precioCobrado; // Lo que realmente le cobra al cliente en la factura

    // Relación: Muchos repuestos pertenecen a una Orden de Trabajo
    @ManyToOne
    @JoinColumn(name = "orden_id")
    @JsonIgnore // Evita bucles infinitos al mandar los datos a React
    private OrdenTrabajo ordenTrabajo;
}