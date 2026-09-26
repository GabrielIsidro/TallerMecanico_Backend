package com.taller.backend.modules.talleres.dto;

import com.taller.backend.modules.talleres.model.ItemOrden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class NuevaOrdenRequest {

    @NotNull(message = "El ID del vehículo es obligatorio")
    private Long vehiculoId;

    @NotBlank(message = "La descripción de la orden es obligatoria")
    private String descripcion;

    private String observacionesMecanico;
    private String observacionesCliente;
    private String checklist;
    private Integer kilometraje;

    @Valid
    private List<ItemOrden> items;
}
