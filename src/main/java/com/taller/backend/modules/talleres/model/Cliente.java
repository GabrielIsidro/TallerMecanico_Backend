package com.taller.backend.modules.talleres.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.taller.backend.core.model.AuditableEntity;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.TenantId;

import java.util.List;

/**
 * Clase que representa un cliente en el taller mecánico.
 */
@Entity
@Table(name = "clientes")
@Data
@EqualsAndHashCode(callSuper = true)
public class Cliente extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    @Column(nullable = false)
    private String nombreCliente;

    @Email(message = "El formato de correo electrónico no es válido")
    private String email;

    private String telefono;
    private String direccion;
    private Boolean esEmpresa;
    private String documentoCuit;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @ToString.Exclude
    @JsonIgnoreProperties("cliente")
    private List<Vehiculo> vehiculos;

    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    @JsonIgnore
    private Taller taller;

    public Taller getTaller() { 
        return taller;
    }
    public void setTaller(Taller taller){
        this.taller = taller;
    }
}
