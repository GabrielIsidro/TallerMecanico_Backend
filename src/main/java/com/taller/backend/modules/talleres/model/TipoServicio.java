package com.taller.backend.modules.talleres.model;
import org.hibernate.annotations.TenantId;
import com.taller.backend.modules.backoffice.model.Taller;

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

    private Double precioSugerido; // Precio de referencia para el servicio

    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    private Taller taller;

}
