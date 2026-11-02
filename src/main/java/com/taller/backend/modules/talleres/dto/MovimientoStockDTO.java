package com.taller.backend.modules.talleres.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.taller.backend.modules.talleres.model.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoStockDTO {

    private Long id;
    private Long repuestoId;
    private String repuestoNombre;
    private String repuestoSku;
    private TipoMovimiento tipo;
    private Integer cantidad;
    private Integer stockPrevio;
    private Integer stockPosterior;
    private String motivo;
    private String usuarioNombre;
    private Long ordenId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fecha;
}
