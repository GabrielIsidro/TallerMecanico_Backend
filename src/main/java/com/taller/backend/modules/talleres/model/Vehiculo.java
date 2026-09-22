package com.taller.backend.modules.talleres.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taller.backend.core.model.AuditableEntity;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.TenantId;

import java.util.List;

@Entity
@Table(name = "vehiculos")
@Data
@EqualsAndHashCode(callSuper = true)
public class Vehiculo extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "La marca es obligatoria")
    @Column(nullable = false)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Column(nullable = false)
    private String modelo;

    @NotBlank(message = "La patente es obligatoria")
    @Column(nullable = false)
    private String patente;

    private String color;
    private int anio;
    private String numeroMotor;
    private String numeroChasis;
    private int kilometraje;
    private int proximoServiceKm;

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @ToString.Exclude
    @JsonIgnoreProperties("vehiculos")
    private Cliente cliente;

    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.REMOVE)
    @JsonIgnore
    private List<OrdenTrabajo> ordenes;

    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    @JsonIgnore
    private Taller taller;

    public Taller getTaller() { return taller; }
    public void setTaller(Taller taller) { this.taller = taller; }
}
