package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.dto.InventarioKpisDTO;
import com.taller.backend.modules.talleres.model.MovimientoStock;
import com.taller.backend.modules.talleres.model.Repuesto;
import com.taller.backend.modules.talleres.model.TipoMovimiento;
import com.taller.backend.modules.talleres.repository.MovimientoStockRepository;
import com.taller.backend.modules.talleres.repository.RepuestoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepuestoServiceTest {

    @Mock
    private RepuestoRepository repuestoRepository;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private RepuestoService repuestoService;

    private Taller tallerMock;
    private Repuesto repuestoMock;

    @BeforeEach
    void setUp() {
        tallerMock = new Taller();
        tallerMock.setId(1L);
        tallerMock.setNombre("Taller Central");

        repuestoMock = new Repuesto();
        repuestoMock.setId(10L);
        repuestoMock.setNombre("Filtro de Aceite");
        repuestoMock.setSku("FLT-001");
        repuestoMock.setMarca("Bosch");
        repuestoMock.setCategoria("Filtros");
        repuestoMock.setUbicacion("Estante A-1");
        repuestoMock.setCantidad(10);
        repuestoMock.setStockMinimo(3);
        repuestoMock.setPrecioCosto(BigDecimal.valueOf(3000));
        repuestoMock.setPrecio(BigDecimal.valueOf(6000));
        repuestoMock.setTaller(tallerMock);
        repuestoMock.setTallerId(1L);
    }

    @Test
    @DisplayName("Debe guardar un repuesto nuevo y registrar su stock inicial de entrada")
    void debeGuardarRepuestoYRegistrarMovimientoInicial() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(repuestoRepository.save(any(Repuesto.class))).thenAnswer(i -> {
            Repuesto r = i.getArgument(0);
            r.setId(10L);
            return r;
        });

        Repuesto guardado = repuestoService.guardarRepuesto(repuestoMock);

        assertNotNull(guardado);
        assertEquals(1L, guardado.getTallerId());
        verify(repuestoRepository, times(1)).save(any(Repuesto.class));
        verify(movimientoStockRepository, times(1)).save(any(MovimientoStock.class));
    }

    @Test
    @DisplayName("Debe obtener un repuesto por ID validando taller")
    void debeObtenerRepuestoPorIdExitosamente() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(repuestoMock));

        Repuesto encontrado = repuestoService.obtenerPorId(10L);

        assertNotNull(encontrado);
        assertEquals("Filtro de Aceite", encontrado.getNombre());
    }

    @Test
    @DisplayName("Debe rechazar repuesto de otro taller")
    void debeRechazarRepuestoDeOtroTaller() {
        repuestoMock.setTallerId(99L);
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(repuestoMock));

        assertThrows(UnauthorizedAccessException.class, () -> repuestoService.obtenerPorId(10L));
    }

    @Test
    @DisplayName("Debe realizar entrada de stock y registrar movimiento")
    void debeAjustarStockEntradaCorrectamente() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(repuestoMock));
        when(repuestoRepository.save(any(Repuesto.class))).thenAnswer(i -> i.getArgument(0));

        Repuesto actualizado = repuestoService.ajustarStock(10L, TipoMovimiento.ENTRADA, 5, "Compra remito #100", null);

        assertEquals(15, actualizado.getCantidad());
        verify(movimientoStockRepository, times(1)).save(any(MovimientoStock.class));
    }

    @Test
    @DisplayName("Debe rechazar salida si el stock es insuficiente")
    void debeAjustarStockSalidaYRechazarSiInsuficiente() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(repuestoRepository.findById(10L)).thenReturn(Optional.of(repuestoMock));

        assertThrows(IllegalArgumentException.class, () -> 
            repuestoService.ajustarStock(10L, TipoMovimiento.SALIDA, 20, "Consumo excesivo", null)
        );
    }

    @Test
    @DisplayName("Debe calcular KPIs de inventario correctamente")
    void debeCalcularKpisCorrectamente() {
        Repuesto r1 = new Repuesto();
        r1.setCantidad(5);
        r1.setStockMinimo(2);
        r1.setPrecioCosto(BigDecimal.valueOf(1000));
        r1.setPrecio(BigDecimal.valueOf(2500));

        Repuesto r2 = new Repuesto();
        r2.setCantidad(1);
        r2.setStockMinimo(3); // Bajo stock
        r2.setPrecioCosto(BigDecimal.valueOf(2000));
        r2.setPrecio(BigDecimal.valueOf(4000));

        Repuesto r3 = new Repuesto();
        r3.setCantidad(0); // Agotado
        r3.setStockMinimo(1);
        r3.setPrecioCosto(BigDecimal.valueOf(500));
        r3.setPrecio(BigDecimal.valueOf(1000));

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(repuestoRepository.findByTallerId(1L)).thenReturn(List.of(r1, r2, r3));

        InventarioKpisDTO kpis = repuestoService.obtenerKpisInventario();

        assertEquals(3, kpis.getTotalArticulos());
        assertEquals(6, kpis.getTotalUnidades()); // 5 + 1 + 0
        assertEquals(new BigDecimal("7000"), kpis.getValorInvertidoCosto()); // 5*1000 + 1*2000
        assertEquals(new BigDecimal("16500"), kpis.getValorVentaPotencial()); // 5*2500 + 1*4000
        assertEquals(new BigDecimal("9500"), kpis.getGananciaEstimada()); // 16500 - 7000
        assertEquals(2, kpis.getArticulosBajoStock()); // r2 (1 <= 3) and r3 (0 <= 1)
        assertEquals(1, kpis.getArticulosAgotados()); // r3
    }
}
