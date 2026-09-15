package com.taller.backend.modules.talleres.model;
import org.hibernate.annotations.TenantId;
import com.taller.backend.modules.backoffice.model.Taller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties; // Importa la anotación @JsonIgnoreProperties de Jackson para evitar problemas de serialización y deserialización con relaciones bidireccionales
import jakarta.persistence.*; // Importa las anotaciones de JPA para definir la entidad y sus propiedades
import lombok.Data; // Importa la anotación @Data de Lombok para generar automáticamente getters, setters, toString, equals y hashCode
import lombok.ToString;
import java.util.List; // Importa la clase List de Java para manejar colecciones de órdenes relacionadas con el vehículo

@Entity // Anotación que le dice a Spring que esta clase es una entidad JPA
@Table(name = "vehiculos") // Anotación que especifica el nombre de la tabla en la base de datos
@Data // Anotación de Lombok que genera automáticamente getters, setters, toString, equals y hashCode

public class Vehiculo { // Clase que representa la entidad Vehiculo en la base de datos

    @Id // Anotación que indica que este campo es la clave primaria de la entidad
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Anotación que indica que el valor de este campo se generará automáticamente por la base de datos, utilizando una estrategia de identidad (auto-incremental)
    private Long id; // Campo que representa el ID del vehículo, es la clave primaria y se genera automáticamente por la base de datos

    

    private String marca; // Campo que representa la marca del vehículo
    private String modelo; // Campo que representa el modelo del vehículo
    private String patente; // Campo que representa la patente del vehículo
    private String color; // Campo que representa el color del vehículo
    private int anio; // Campo que representa el año del vehículo
    private String numeroMotor; // Campo que representa el número de motor del vehículo
    private String numeroChasis; // Campo que representa el número de chasis del vehículo
    private int kilometraje; // Campo que representa el kilometraje del vehículo
    private int proximoServiceKm; // Campo que representa el kilometraje para el próximo service del vehículo

    //RELACION: Un vehiculo tiene muchas ordenes de trabajo, pero cada orden de trabajo pertenece a un solo vehiculo

    @ManyToOne // Anotación que indica que esta entidad tiene una relación de muchos a uno con la entidad Cliente
    @JoinColumn(name = "cliente_id") // Anotación que especifica el nombre de la columna en la tabla de vehículos que se usará para la relación con la tabla de clientes
    @ToString.Exclude // Evita que Lombok incluya este campo en el método toString para prevenir recursión infinita
    @JsonIgnoreProperties("vehiculos") // Evita problemas de serialización y deserialización con relaciones bidireccionales, ignorando la propiedad "vehiculos" en la clase Cliente
    private Cliente cliente; // Campo que representa la relación con la entidad Cliente, es decir,

    // Esto le dice a Java: "Si borras este auto, llévate todas sus órdenes con él"
    @OneToMany(mappedBy = "vehiculo", cascade = CascadeType.REMOVE)
    private List<OrdenTrabajo> ordenes;

    // Muchos vehículos están registrados en un taller
    @TenantId
    @Column(name = "taller_id")
    private Long tallerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taller_id", insertable = false, updatable = false)
    private Taller taller;

    public Taller getTaller() { return taller; }
    public void setTaller(Taller taller) { this.taller = taller; }
}
