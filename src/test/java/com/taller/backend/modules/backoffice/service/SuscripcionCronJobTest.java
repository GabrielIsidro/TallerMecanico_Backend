package com.taller.backend.modules.backoffice.service;

import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuscripcionCronJobTest {

    @Mock
    private TallerRepository tallerRepository;

    @InjectMocks
    private SuscripcionCronJob cronJob;

    private Taller tallerActivo;
    private Taller tallerPorVencer;
    private Taller tallerVencidoGracia;
    private Taller tallerExpiradoLargo;
    private Taller tallerConCancelacionProgramada;

    @BeforeEach
    void setUp() {
        LocalDate hoy = LocalDate.now();

        // 1. Taller con 25 días restantes (Activo)
        tallerActivo = new Taller();
        tallerActivo.setId(1L);
        tallerActivo.setNombre("Taller Activo");
        tallerActivo.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        tallerActivo.setFechaVencimiento(hoy.plusDays(25));
        tallerActivo.setCancelacionProgramada(false);

        // 2. Taller que vence hoy (Aún no expiró)
        tallerPorVencer = new Taller();
        tallerPorVencer.setId(2L);
        tallerPorVencer.setNombre("Taller Por Vencer");
        tallerPorVencer.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        tallerPorVencer.setFechaVencimiento(hoy);
        tallerPorVencer.setCancelacionProgramada(false);

        // 3. Taller vencido hace 2 días (dentro de los 3 días de gracia)
        tallerVencidoGracia = new Taller();
        tallerVencidoGracia.setId(3L);
        tallerVencidoGracia.setNombre("Taller En Gracia");
        tallerVencidoGracia.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        tallerVencidoGracia.setFechaVencimiento(hoy.minusDays(2));
        tallerVencidoGracia.setCancelacionProgramada(false);

        // 4. Taller vencido hace 5 días (superó el período de gracia)
        tallerExpiradoLargo = new Taller();
        tallerExpiradoLargo.setId(4L);
        tallerExpiradoLargo.setNombre("Taller Expirado");
        tallerExpiradoLargo.setEstadoSuscripcion(EstadoSuscripcion.VENCIDA);
        tallerExpiradoLargo.setFechaVencimiento(hoy.minusDays(5));
        tallerExpiradoLargo.setCancelacionProgramada(false);

        // 5. Taller con cancelación programada que venció ayer
        tallerConCancelacionProgramada = new Taller();
        tallerConCancelacionProgramada.setId(5L);
        tallerConCancelacionProgramada.setNombre("Taller Cancelación Programada");
        tallerConCancelacionProgramada.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        tallerConCancelacionProgramada.setFechaVencimiento(hoy.minusDays(1));
        tallerConCancelacionProgramada.setCancelacionProgramada(true);
    }

    @Test
    @DisplayName("El cronjob no debe modificar talleres con fechas vigentes")
    void testNoModificaTalleresVigentes() {
        when(tallerRepository.findAll()).thenReturn(List.of(tallerActivo, tallerPorVencer));

        cronJob.revisarSuscripcionesDiariamente();

        assertEquals(EstadoSuscripcion.ACTIVA, tallerActivo.getEstadoSuscripcion());
        assertEquals(EstadoSuscripcion.ACTIVA, tallerPorVencer.getEstadoSuscripcion());
        verify(tallerRepository, never()).save(any(Taller.class));
    }

    @Test
    @DisplayName("Taller vencido entre 1 y 3 días pasa a estado VENCIDA (Período de Gracia)")
    void testPasaAVencidaEnPeriodoDeGracia() {
        when(tallerRepository.findAll()).thenReturn(List.of(tallerVencidoGracia));

        cronJob.revisarSuscripcionesDiariamente();

        assertEquals(EstadoSuscripcion.VENCIDA, tallerVencidoGracia.getEstadoSuscripcion());
        verify(tallerRepository).save(tallerVencidoGracia);
    }

    @Test
    @DisplayName("Taller vencido hace más de 3 días pasa a estado SUSPENDIDA")
    void testPasaASuspendidaAlSuperarGracia() {
        when(tallerRepository.findAll()).thenReturn(List.of(tallerExpiradoLargo));

        cronJob.revisarSuscripcionesDiariamente();

        assertEquals(EstadoSuscripcion.SUSPENDIDA, tallerExpiradoLargo.getEstadoSuscripcion());
        verify(tallerRepository).save(tallerExpiradoLargo);
    }

    @Test
    @DisplayName("Taller con cancelación programada se suspende directamente al vencer sin gracia")
    void testCancelacionProgramadaSeSuspendeSinGracia() {
        when(tallerRepository.findAll()).thenReturn(List.of(tallerConCancelacionProgramada));

        cronJob.revisarSuscripcionesDiariamente();

        assertEquals(EstadoSuscripcion.SUSPENDIDA, tallerConCancelacionProgramada.getEstadoSuscripcion());
        verify(tallerRepository).save(tallerConCancelacionProgramada);
    }

    @Test
    @DisplayName("Verificación matemática de cálculo de días decrecientes con el paso del tiempo")
    void testCalculoDiasDecrecientesInmutableAReinicios() {
        LocalDate fechaInicio = LocalDate.of(2026, 11, 1);
        LocalDate fechaVencimiento = fechaInicio.plusDays(30); // 2026-12-01

        // Día 1
        LocalDate dia1 = fechaInicio;
        long diasRestantesDia1 = ChronoUnit.DAYS.between(dia1, fechaVencimiento);
        assertEquals(30, diasRestantesDia1, "El día 1 deben quedar 30 días");

        // Día 10 (simulando 10 días después de haber apagado el servidor)
        LocalDate dia10 = fechaInicio.plusDays(10);
        long diasRestantesDia10 = ChronoUnit.DAYS.between(dia10, fechaVencimiento);
        assertEquals(20, diasRestantesDia10, "El día 10 deben quedar 20 días");

        // Día 29
        LocalDate dia29 = fechaInicio.plusDays(29);
        long diasRestantesDia29 = ChronoUnit.DAYS.between(dia29, fechaVencimiento);
        assertEquals(1, diasRestantesDia29, "El día 29 debe quedar 1 día");

        // Día 30 (Día del vencimiento)
        LocalDate dia30 = fechaInicio.plusDays(30);
        long diasRestantesDia30 = ChronoUnit.DAYS.between(dia30, fechaVencimiento);
        assertEquals(0, diasRestantesDia30, "El día 30 deben quedar 0 días");

        // Día 31 (Vencido)
        LocalDate dia31 = fechaInicio.plusDays(31);
        long diasRestantesDia31 = ChronoUnit.DAYS.between(dia31, fechaVencimiento);
        assertEquals(-1, diasRestantesDia31, "El día 31 debe indicar -1 día (vencido)");
    }
}
