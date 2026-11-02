package com.taller.backend.modules.talleres.dto;

import com.taller.backend.modules.talleres.model.TipoMovimiento;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AjusteStockRequest {

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private TipoMovimiento tipo; // ENTRADA, SALIDA, AJUSTE

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;

    private String motivo; // Ej: "Compra remito #4512", "Ajuste físico de inventario", "Merma/Rotura"
}
