package com.taller.backend.modules.talleres.dto;

import lombok.Data;

@Data
public class RepuestoOrdenRequest {

    private Long repuestoId; // Opcional, ID del repuesto en inventario
    private String descripcion;
    private String proveedor;
    private Integer cantidad;
    private Double precioUnitario;
    private Double costoUnitario;
}
