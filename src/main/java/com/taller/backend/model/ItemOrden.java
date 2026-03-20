package com.taller.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

// import java.util.List;

/**
 * 
 * Entidad que representa un item en una orden de trabajo
 * Cada item corresponde a un servicio específico que se realizará en el vehículo, con su cantidad y subtotal calculado
 * El subtotal se calcula multiplicando el precio del servicio (ajustado por la categoría del vehículo) por la cantidad de ese servicio en la orden
 * El precio del servicio se determina según la categoría del vehículo (A, B o C) y el tipo de servicio (mano de obra, repuesto, etc.)
 * La relación con OrdenTrabajo es ManyToOne, ya que una orden puede tener múltiples items, pero cada item pertenece a una sola orden
 * La relación con TipoServicio es ManyToOne, ya que un tipo de servicio puede ser utilizado en múltiples items, pero cada item corresponde a un solo tipo de servicio
 * El campo subtotal se actualiza automáticamente cada vez que se asigna un tipo de servicio o se cambia la cantidad, asegurando que siempre refleje el costo correcto del item en la orden
 * 
 */
@Entity
@Table(name = "items_orden")
@Data
public class ItemOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "orden_id")
    @ToString.Exclude
    @JsonIgnoreProperties("items") // Evita la recursión infinita al serializar a JSON
    private OrdenTrabajo orden;

    @ManyToOne
    @JoinColumn(name = "tipo_servicio_id")
    private TipoServicio tipoServicio;

    private Integer cantidad;
    private Double subtotal;

    /*
    Método para calcular el subtotal del item basado en la categoría del vehículo
     */
    public void calcularSubtotal(CategoriaVehiculo categoriaVehiculo) {
        double precioUnitario = 0.0; // Precio base, se ajustará según la categoría del vehículo

        //1. Si es mano de obra (ATAIA)
        if (this.tipoServicio != null && categoriaVehiculo != null) {
            switch (categoriaVehiculo) {
                case CATEGORIA_A: precioUnitario = tipoServicio.getPrecioA(); break;
                case CATEGORIA_B: precioUnitario = tipoServicio.getPrecioB(); break;
                case CATEGORIA_C: precioUnitario = tipoServicio.getPrecioC(); break;
            }
        }

        if (this.cantidad == null) this.cantidad = 1;
        this.subtotal = precioUnitario * this.cantidad;
    }
}
