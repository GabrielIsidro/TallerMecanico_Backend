package com.taller.backend.modules.talleres.dto;

import com.taller.backend.modules.talleres.model.ItemOrden;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class ActualizarOrdenRequest {

    private Long vehiculoId;
    private String descripcion;
    private String observacionesMecanico;
    private String observacionesCliente;
    private String checklist;
    private Integer kilometraje;

    @Valid
    private List<ItemOrden> items;
}
