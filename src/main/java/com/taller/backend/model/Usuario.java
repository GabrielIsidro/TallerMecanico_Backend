package com.taller.backend.model;

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
    private String password; 

    private String nombre;
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    // ---> LA MAGIA DEL SAAS <---
    // Muchos usuarios pueden pertenecer a un mismo taller.
    // nullable = true es VITAL, porque vos (el SUPER_ADMIN) no pertenecés a un taller específico, sos el dueño de todo.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", nullable = true)
    private Taller taller;
}