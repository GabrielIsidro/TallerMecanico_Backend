package com.taller.backend.modules.talleres.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimientos_stock")
@Data
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    @JsonIgnore
    private Taller taller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "repuesto_id", nullable = false)
    @JsonIgnoreProperties({"taller", "movimientos"})
    private Repuesto repuesto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Integer cantidad;

    private Integer stockPrevio;

    private Integer stockPosterior;

    private String motivo;

    private String usuarioNombre;

    private Long ordenId;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fecha;

    public MovimientoStock(Repuesto repuesto, Long tallerId, TipoMovimiento tipo, Integer cantidad, 
                           Integer stockPrevio, Integer stockPosterior, String motivo, String usuarioNombre, Long ordenId) {
        this.repuesto = repuesto;
        this.tallerId = tallerId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockPrevio = stockPrevio;
        this.stockPosterior = stockPosterior;
        this.motivo = motivo;
        this.usuarioNombre = usuarioNombre;
        this.ordenId = ordenId;
        this.fecha = LocalDateTime.now();
    }
}
