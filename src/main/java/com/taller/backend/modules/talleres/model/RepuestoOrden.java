package com.taller.backend.modules.talleres.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private String proveedor;   // Ej: "Repuestos Carlitos" o "Stock Taller"
    
    private Integer cantidad = 1;
    private Double precioUnitario;
    private Double costoUnitario;
    
    private Double costo;         // Subtotal costo
    private Double precioCobrado; // Subtotal cobrado

    @Column(name = "repuesto_id")
    private Long repuestoId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "repuesto_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"taller", "movimientos"})
    private Repuesto repuestoOriginal;

    // Relación: Muchos repuestos pertenecen a una Orden de Trabajo
    @ManyToOne
    @JoinColumn(name = "orden_id")
    @JsonIgnore // Evita bucles infinitos al mandar los datos a React
    private OrdenTrabajo ordenTrabajo;

    public void calcularSubtotales() {
        if (cantidad == null || cantidad <= 0) {
            cantidad = 1;
        }
        if (precioUnitario != null) {
            this.precioCobrado = precioUnitario * cantidad;
        } else if (this.precioCobrado != null) {
            this.precioUnitario = this.precioCobrado / cantidad;
        }
        if (costoUnitario != null) {
            this.costo = costoUnitario * cantidad;
        } else if (this.costo != null) {
            this.costoUnitario = this.costo / cantidad;
        }
    }
}
