package com.taller.backend.modules.talleres.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.TenantId;

/**
 * Clase que representa un tipo de servicio en el taller mecánico.
 */
@Entity
@Table(name = "tipo_servicio")
@Data 
public class TipoServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El grupo es obligatorio")
    @Column(nullable = false)
    private String grupo;

    @NotBlank(message = "La descripción es obligatoria")
    @Column(length = 500, nullable = false)
    private String descripcion;

    @NotNull(message = "El precio sugerido es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio no puede ser negativo")
    @Column(nullable = false)
    private Double precioSugerido;

    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    @JsonIgnore
    private Taller taller;
}
