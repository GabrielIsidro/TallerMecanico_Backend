package com.taller.backend.modules.talleres.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taller.backend.core.model.AuditableEntity;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.TenantId;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordenes_trabajo")
@Data
@EqualsAndHashCode(callSuper = true)
public class OrdenTrabajo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaIngreso; 

    private String descripcion; 

    @Column(columnDefinition = "TEXT")
    private String observacionesMecanico; 

    @Column(columnDefinition = "TEXT")
    private String observacionesCliente; 

    @Column(columnDefinition = "TEXT")
    private String checklist; 

    private Double costoTotal; 
    
    @Enumerated(EnumType.STRING) 
    private EstadoOrden estado; 

    @Enumerated(EnumType.STRING) 
    private FormaPago formaPago;

    @ManyToOne
    @JoinColumn(name = "vehiculo_id") 
    @JsonIgnoreProperties("ordenes") 
    private Vehiculo vehiculo;

    @ToString.Exclude 
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true) 
    @JsonIgnoreProperties("orden") 
    private List<ItemOrden> items = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "ordenTrabajo", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("ordenTrabajo") 
    private List<RepuestoOrden> repuestos = new ArrayList<>();

    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    @JsonIgnore
    private Taller taller;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (fechaIngreso == null) {
            fechaIngreso = LocalDateTime.now();
        }
        Double totalItems = getTotal();
        if (totalItems > 0.0) {
            this.costoTotal = totalItems;
        } else if (this.costoTotal == null) {
            this.costoTotal = 0.0;
        }
    }

    public void agregarItem(ItemOrden item) {
        items.add(item);
        item.setOrden(this); 
    }

    public void agregarRepuesto(RepuestoOrden repuesto) {
        repuestos.add(repuesto);
        repuesto.setOrdenTrabajo(this);
    }

    public Double getTotal() {
        double total = 0.0;
        
        for (ItemOrden item : items){
            if (item.getSubtotal() != null) {
                total += item.getSubtotal();
            }
        }
        
        for (RepuestoOrden repuesto : repuestos) {
            if (repuesto.getPrecioCobrado() != null) {
                total += repuesto.getPrecioCobrado();
            }
        }
        
        return total;
    }

    public Taller getTaller() { return taller; }
    public void setTaller(Taller taller) { this.taller = taller; }
}
