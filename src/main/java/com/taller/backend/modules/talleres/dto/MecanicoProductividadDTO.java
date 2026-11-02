package com.taller.backend.modules.talleres.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MecanicoProductividadDTO {
    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private int ordenesEnCurso;
    private int finalizadosHoy;
    private int entregadosHoy;
    private int totalHistoricoFinalizados;
    private List<OrdenActivaMecanicoDTO> ordenesActivas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrdenActivaMecanicoDTO {
        private Long ordenId;
        private String patente;
        private String marca;
        private String modelo;
        private String clienteNombre;
        private String estado;
        private String descripcion;
    }
}
