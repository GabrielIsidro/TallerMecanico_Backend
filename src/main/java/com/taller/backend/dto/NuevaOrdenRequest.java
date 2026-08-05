package com.taller.backend.dto;

import com.taller.backend.model.ItemOrden;
import lombok.Data;
import java.util.List;

@Data
public class NuevaOrdenRequest {
    private Long vehiculoId;
    private String descripcion;
    private List<ItemOrden> items;
}