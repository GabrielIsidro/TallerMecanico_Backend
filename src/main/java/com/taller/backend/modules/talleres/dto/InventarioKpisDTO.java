package com.taller.backend.modules.talleres.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventarioKpisDTO {

    private int totalArticulos;
    private int totalUnidades;
    private BigDecimal valorInvertidoCosto;
    private BigDecimal valorVentaPotencial;
    private BigDecimal gananciaEstimada;
    private int articulosBajoStock;
    private int articulosAgotados;
}
