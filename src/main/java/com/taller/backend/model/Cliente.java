package com.taller.backend.model;

import jakarta.persistence.*; // Importa las anotaciones de JPA para definir la entidad y sus propiedades
import lombok.Data;
import lombok.ToString;

import java.util.List; // Importa la clase List para manejar colecciones de objetos

/**
 * Clase que representa un cliente en el taller mecánico.
 * Contiene información sobre el nombre, apellido, email, teléfono y dirección del cliente.
 * También tiene una relación de uno a muchos con la entidad Vehiculo.
 */
@Entity // Anotación que le dice a Spring que esta clase es una entidad JPA
@Table(name = "clientes") // Anotación que especifica el nombre de la tabla en la base de datos
@Data // Anotación de Lombok que genera automáticamente getters, setters, toString, equals y hashCode
public class Cliente {

    @Id // Anotación que indica que este campo es la clave primaria de la entidad
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Campo que representa el ID del cliente, es la clave primaria y se genera automáticamente por la base de datos

    private String nombreCliente;
    private String email; // Campo que representa el email del cliente
    private String telefono; // Campo que representa el teléfono del cliente
    private String direccion; // Campo que representa la dirección del cliente
    private Boolean esEmpresa;
    private String documentoCuit;

    //RELACION: Un cliente tiene muchos vehiculos
    //"mappedBy" dice: "El dueño de la relacion es el campo 'cliente' en la clase Vehiculo"
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL)
    @ToString.Exclude // Evita que Lombok incluya esta lista en el método toString para prevenir recursión infinita
    private List<Vehiculo> vehiculos; // Campo que representa la lista de vehículos asociados a este cliente, es una relación de uno a muchos con la entidad Vehiculo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id")
    private Taller taller;

    public Taller getTaller() { 
        return taller;
    }
    public void setTaller(Taller taller){
        this.taller = taller;
    }

}
