package com.taller.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "ordenes_trabajo")
@Data
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaIngreso; 

    private String descripcion; 

    // ---> NUEVO: El campo para observaciones largas
    @Column(columnDefinition = "TEXT")
    private String observaciones; 

    private Double costoTotal; 
    
    @Enumerated(EnumType.STRING) 
    private EstadoOrden estado; 

    @Enumerated(EnumType.STRING) 
    private FormaPago formaPago;

    // RELACIÓN: Una orden pertenece a un Vehiculo
    @ManyToOne
    @JoinColumn(name = "vehiculo_id") 
    @JsonIgnoreProperties("ordenes") 
    private Vehiculo vehiculo;

    // RELACIÓN: Lista de Servicios (Mano de obra)
    @ToString.Exclude 
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true) 
    @JsonIgnoreProperties("orden") 
    private List<ItemOrden> items = new ArrayList<>();

    // ---> NUEVO: Lista de Repuestos dinámicos
    @ToString.Exclude
    @OneToMany(mappedBy = "ordenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("ordenTrabajo") 
    private List<RepuestoOrden> repuestos = new ArrayList<>();

    // Muchas órdenes son emitidas por un taller
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id")
    private Taller taller;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (fechaIngreso == null) {
            fechaIngreso = LocalDateTime.now();
        }
        // Actualiza el costo total sumando servicios + repuestos
        this.costoTotal = getTotal(); 
    }

    public void agregarItem(ItemOrden item) {
        items.add(item);
        item.setOrden(this); 
    }

    // ---> NUEVO: Método helper para agregar repuestos a la orden
    public void agregarRepuesto(RepuestoOrden repuesto) {
        repuestos.add(repuesto);
        repuesto.setOrdenTrabajo(this);
    }

    /**
     * ---> ACTUALIZADO: Ahora suma los servicios Y los repuestos
     */
    public Double getTotal() {
        double total = 0.0;
        
        // 1. Sumar la mano de obra (Items de servicio)
        for (ItemOrden item : items){
            if (item.getSubtotal() != null) {
                total += item.getSubtotal();
            }
        }
        
        // 2. Sumar los repuestos (Precio cobrado al cliente)
        for (RepuestoOrden repuesto : repuestos) {
            if (repuesto.getPrecioCobrado() != null) {
                total += repuesto.getPrecioCobrado();
            }
        }
        
        return total;
    }

    // Setters y Getters explícitos del Taller (Lombok ya los hace, pero los dejamos por si los estabas usando)
    public Taller getTaller() { return taller; }
    public void setTaller(Taller taller) { this.taller = taller; }
}