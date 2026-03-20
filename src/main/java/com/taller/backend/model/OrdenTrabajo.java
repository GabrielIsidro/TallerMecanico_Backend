package com.taller.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Clase que representa una orden de trabajo en el taller mecánico.
 * Una orden de trabajo contiene información sobre el vehículo, la fecha de ingreso, la descripción del trabajo a realizar, el estado de la orden, los items asociados (servicios y repuestos) y el costo total calculado a partir de esos items.
 * La relación con Vehiculo es ManyToOne, ya que un vehículo puede tener múltiples órdenes de trabajo, pero cada orden pertenece a un solo vehículo.
 * La relación con ItemOrden es OneToMany, ya que una orden de trabajo puede tener múltiples items, pero cada item pertenece a una sola orden.
 * El campo costoTotal se actualiza automáticamente cada vez que se agrega o modifica un item en la orden, asegurando que siempre refleje el costo correcto de la orden de trabajo.
 * El campo fechaIngreso se establece automáticamente al crear una nueva orden de trabajo, pero también se puede modificar si es necesario.
 * El campo estado utiliza un enum para representar los diferentes estados que puede tener una orden de trabajo (PENDIENTE, EN_PROGRESO, COMPLETADA, CANCELADA), lo que facilita la gestión del flujo de trabajo en el taller.
 */
@Entity
@Table(name = "ordenes_trabajo")
@Data
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Identificador único de la orden de trabajo

    //Para que la fecha se guarde automaticamente al crear una nueva orden de trabajo
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaIngreso; // Fecha de ingreso de la orden de trabajo

    private String descripcion; // Descripción de la orden de trabajo

    private Double costoTotal; // Costo total de la orden de trabajo, calculado a partir de los repuestos y servicios asociados
    
    @Enumerated(EnumType.STRING) // Guarda el nombre del estado en la BD
    private EstadoOrden estado; // Estado de la orden de trabajo, usando el enum EstadoOrden

    //RELACION: Una orden pertece a un Vehiculo
    @ManyToOne
    @JoinColumn(name = "vehiculo_id") // Nombre de la columna que se usará como clave foránea en la tabla ordenes_trabajo
    @JsonIgnoreProperties("ordenes") // Para evitar la referencia circular al serializar a JSON
    private Vehiculo vehiculo;

    @ToString.Exclude // Para evitar la referencia circular al imprimir el objeto
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true) // Relación uno a muchos con ItemOrden
    @JsonIgnoreProperties("orden") // Para evitar la referencia circular al serializar a JSON
    private List<ItemOrden> items = new ArrayList<>();

    /**
     * Método que se ejecuta antes de guardar o actualizar la orden de trabajo.
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (fechaIngreso == null) {
            fechaIngreso = LocalDateTime.now();
        }
        this.costoTotal = getTotal(); // Actualiza el costo total antes de guardar o actualizar la orden
    }

    /**
     * Método para agregar un item a la orden de trabajo.
     * @param item El item a agregar.
     */
    public void agregarItem(ItemOrden item) {
        items.add(item);
        item.setOrden(this); // Establece la relación bidireccional
    }

    /**
     * Método para obtener el costo total de la orden de trabajo.
     * @return El costo total de la orden.
     */
    public Double getTotal() {
        double total = 0.0;
        for (ItemOrden item : items){
            if (item.getSubtotal() != null) {
                total += item.getSubtotal();
            }
        }
        return total;
    }

}
