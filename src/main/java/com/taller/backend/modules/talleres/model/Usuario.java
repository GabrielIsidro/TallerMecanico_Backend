package com.taller.backend.modules.talleres.model;
import org.hibernate.annotations.TenantId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.taller.backend.modules.backoffice.model.Taller;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuarios")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El email va a ser el nombre de usuario para iniciar sesión
    @Column(unique = true, nullable = false)
    private String email;

    // Acá NO se guarda "123456", Spring Security va a guardar un código encriptado
    @Column(nullable = false)
    @JsonIgnore
    private String password; 

    private String nombre;
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    @Column(name = "debe_cambiar_password", nullable = true)
    private Boolean debeCambiarPassword = false;

    @Column(name = "codigo_recuperacion")
    private String codigoRecuperacion;

    @Column(name = "codigo_recuperacion_expiracion")
    private java.time.LocalDateTime codigoRecuperacionExpiracion;

    // ---> LA MAGIA DEL SAAS <---
    // Muchos usuarios pueden pertenecer a un mismo taller.
    // nullable = true es VITAL, porque vos (el SUPER_ADMIN) no pertenecés a un taller específico, sos el dueño de todo.
    @TenantId
    @Column(name = "taller_id", nullable = true)
    private Long tallerId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    @JsonIgnore
    private Taller taller;
}
