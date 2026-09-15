package com.taller.backend.modules.talleres.dto;

import com.taller.backend.modules.talleres.model.ItemOrden;
import lombok.Data;
import java.util.List;

@Data
public class NuevaOrdenRequest {
    private Long vehiculoId;
    private String descripcion;
    private String observacionesMecanico;
    private String observacionesCliente;
    private String checklist;
    private Integer kilometraje;
    private List<ItemOrden> items;
}
