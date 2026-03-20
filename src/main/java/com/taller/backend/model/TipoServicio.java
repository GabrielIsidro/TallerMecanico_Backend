package com.taller.backend.model;

import jakarta.persistence.*;
import lombok.Data;

/**
 * Clase que representa un tipo de servicio en el taller mecánico.
 * Contiene información sobre el grupo al que pertenece, una descripción del servicio y los precios asociados.
 */
@Entity
@Table(name = "tipo_servicio")
@Data 
public class TipoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Identificador único del tipo de servicio

    private String grupo; // Grupo al que pertenece el servicio (por ejemplo, "Mantenimiento", "Reparación", etc.)

    @Column(length = 500)
    private String descripcion; // Descripción detallada del servicio

    private Double precioA; // Precio para categoría A
    private Double precioB; // Precio para categoría B
    private Double precioC; // Precio para categoría C

}
