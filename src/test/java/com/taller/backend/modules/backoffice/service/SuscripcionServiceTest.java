package com.taller.backend.modules.backoffice.service;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.modules.backoffice.model.*;
import com.taller.backend.modules.backoffice.repository.PlanSuscripcionRepository;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuscripcionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private TallerRepository tallerRepository;

    @Mock
    private PlanSuscripcionRepository planRepository;

    @Mock
    private MercadoPagoService mercadoPagoService;

    @InjectMocks
    private SuscripcionService suscripcionService;

    private Taller taller;
    private PlanSuscripcion planMensual;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        taller = new Taller();
        taller.setId(1L);
        taller.setNombre("Taller Mecánico Test");
        taller.setEstadoSuscripcion(EstadoSuscripcion.PRUEBA_GRATUITA);

        planMensual = new PlanSuscripcion();
        planMensual.setId(10L);
        planMensual.setDescripcion("Plan Profesional");
        planMensual.setTipo(TipoPlan.PRO);
        planMensual.setFrecuencia(FrecuenciaPlan.MENSUAL);
        planMensual.setPrecio(20000.0);

        usuario = new Usuario();
        usuario.setId(5L);
        usuario.setEmail("admin@tallertest.com");
        usuario.setTaller(taller);
    }

    @Test
    @DisplayName("Debe generar URL de checkout para usuario y plan válidos")
    void debeGenerarCheckoutUrlExitosamente() {
        when(planRepository.findById(10L)).thenReturn(Optional.of(planMensual));
        when(usuarioRepository.findByEmail("admin@tallertest.com")).thenReturn(Optional.of(usuario));
        when(mercadoPagoService.crearPreferenciaPago(taller, planMensual)).thenReturn("https://mercadopago.fake/checkout");

        String checkoutUrl = suscripcionService.generarCheckoutUrl(10L, "admin@tallertest.com");

        assertNotNull(checkoutUrl);
        assertEquals("https://mercadopago.fake/checkout", checkoutUrl);
        verify(mercadoPagoService).crearPreferenciaPago(taller, planMensual);
    }

    @Test
    @DisplayName("Debe lanzar excepción al generar checkout si el plan no existe")
    void debeLanzarExcepcionSiPlanNoExiste() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                suscripcionService.generarCheckoutUrl(99L, "admin@tallertest.com")
        );
    }

    @Test
    @DisplayName("Debe procesar webhook y actualizar suscripción mensual activa (+30 días)")
    void debeProcesarWebhookYActivarSuscripcionMensual() {
        when(planRepository.findById(10L)).thenReturn(Optional.of(planMensual));
        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));

        Map<String, Object> payload = new HashMap<>();
        payload.put("planId", "10");
        payload.put("tallerId", "1");

        suscripcionService.procesarWebhook(payload, null);

        assertEquals(EstadoSuscripcion.ACTIVA, taller.getEstadoSuscripcion());
        assertEquals(TipoPlan.PRO, taller.getTipoPlan());
        assertNotNull(taller.getFechaVencimiento());
        assertTrue(taller.getFechaVencimiento().isAfter(LocalDate.now().plusDays(28)));
        verify(tallerRepository).save(taller);
    }

    @Test
    @DisplayName("Debe lanzar BusinessRuleException en webhook si falta planId")
    void debeLanzarExcepcionSiWebhookNoTienePlanId() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tallerId", "1");

        assertThrows(BusinessRuleException.class, () ->
                suscripcionService.procesarWebhook(payload, null)
        );
    }

    @Test
    @DisplayName("Debe delegar la validación criptográfica en MercadoPagoService")
    void debeDelegarValidacionCriptografica() {
        when(mercadoPagoService.validarFirmaWebhook("sig", "req-1", "123")).thenReturn(true);

        boolean resultado = suscripcionService.validarFirmaWebhook("sig", "req-1", "123");

        assertTrue(resultado);
        verify(mercadoPagoService).validarFirmaWebhook("sig", "req-1", "123");
    }

    @Test
    @DisplayName("No debe acumular días anteriores al cambiar de plan (Upgrade o cambio de tipo)")
    void noDebeAcumularDiasAlCambiarDePlan() {
        // Taller tenía un plan BASE activo con 20 días por delante
        taller.setTipoPlan(TipoPlan.BASE);
        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        taller.setFechaVencimiento(LocalDate.now().plusDays(20));

        when(planRepository.findById(10L)).thenReturn(Optional.of(planMensual)); // Plan PRO Mensual
        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));

        suscripcionService.actualizarSuscripcionTaller(1L, 10L);

        // El nuevo plan PRO debe comenzar desde HOY (+30 días), NO desde hoy + 20 + 30
        assertEquals(TipoPlan.PRO, taller.getTipoPlan());
        assertEquals(LocalDate.now().plusDays(30), taller.getFechaVencimiento());
        verify(tallerRepository).save(taller);
    }

    @Test
    @DisplayName("Debe acumular días si es renovación del MISMO plan activo")
    void debeAcumularDiasSiEsRenovacionMismoPlan() {
        // Taller tiene Plan PRO activo con 5 días restantes
        taller.setTipoPlan(TipoPlan.PRO);
        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        LocalDate vencimientoOriginal = LocalDate.now().plusDays(5);
        taller.setFechaVencimiento(vencimientoOriginal);

        when(planRepository.findById(10L)).thenReturn(Optional.of(planMensual)); // Plan PRO Mensual
        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));

        suscripcionService.actualizarSuscripcionTaller(1L, 10L);

        // Al renovar el mismo plan, se extiende desde su vencimiento original (+30 días)
        assertEquals(TipoPlan.PRO, taller.getTipoPlan());
        assertEquals(vencimientoOriginal.plusDays(30), taller.getFechaVencimiento());
        verify(tallerRepository).save(taller);
    }

    @Test
    @DisplayName("Debe lanzar excepción al generar checkout si ya tiene el mismo plan activo con más de 7 días")
    void debeLanzarExcepcionSiIntentaRenovarMismoPlanConMasDeSieteDias() {
        taller.setTipoPlan(TipoPlan.PRO);
        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        taller.setFechaVencimiento(LocalDate.now().plusDays(15)); // Le quedan 15 días

        when(planRepository.findById(10L)).thenReturn(Optional.of(planMensual));
        when(usuarioRepository.findByEmail("admin@tallertest.com")).thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                suscripcionService.generarCheckoutUrl(10L, "admin@tallertest.com")
        );

        assertTrue(ex.getMessage().contains("ya se encuentra activo"));
        verify(mercadoPagoService, never()).crearPreferenciaPago(any(), any());
    }

    @Test
    @DisplayName("Debe confirmar pago exitosamente y activar plan")
    void debeConfirmarPagoExitosamente() {
        when(mercadoPagoService.tieneAccessToken()).thenReturn(true);
        Map<String, Object> pagoData = new HashMap<>();
        pagoData.put("status", "approved");
        pagoData.put("external_reference", "taller_1_plan_10");
        when(mercadoPagoService.consultarPago("186602777629")).thenReturn(pagoData);
        when(usuarioRepository.findByEmail("admin@tallertest.com")).thenReturn(Optional.of(usuario));
        when(planRepository.findById(10L)).thenReturn(Optional.of(planMensual));
        when(tallerRepository.findById(1L)).thenReturn(Optional.of(taller));

        Taller resultado = suscripcionService.confirmarPago("186602777629", "admin@tallertest.com");

        assertNotNull(resultado);
        assertEquals(TipoPlan.PRO, resultado.getTipoPlan());
        assertEquals(EstadoSuscripcion.ACTIVA, resultado.getEstadoSuscripcion());
        verify(tallerRepository).save(taller);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el pago no está aprobado")
    void debeLanzarExcepcionSiPagoNoAprobado() {
        when(mercadoPagoService.tieneAccessToken()).thenReturn(true);
        Map<String, Object> pagoData = new HashMap<>();
        pagoData.put("status", "pending");
        pagoData.put("status_detail", "pending_contingency");
        when(mercadoPagoService.consultarPago("12345")).thenReturn(pagoData);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                suscripcionService.confirmarPago("12345", "admin@tallertest.com")
        );

        assertTrue(ex.getMessage().contains("no se encuentra aprobado"));
    }

    @Test
    @DisplayName("Debe programar cancelación de suscripción activa sin suspender de inmediato")
    void debeProgramarCancelacionExitosamenteSinSuspenderDeInmediato() {
        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        LocalDate vencimientoOriginal = LocalDate.now().plusDays(25);
        taller.setFechaVencimiento(vencimientoOriginal);
        when(usuarioRepository.findByEmail("admin@tallertest.com")).thenReturn(Optional.of(usuario));

        suscripcionService.cancelarSuscripcion("admin@tallertest.com");

        // Mantiene el estado ACTIVO y la fecha de vencimiento original
        assertEquals(EstadoSuscripcion.ACTIVA, taller.getEstadoSuscripcion());
        assertEquals(vencimientoOriginal, taller.getFechaVencimiento());
        assertTrue(taller.getCancelacionProgramada());
        verify(tallerRepository).save(taller);
    }

    @Test
    @DisplayName("Debe reactivar suscripción con cancelación programada")
    void debeReactivarSuscripcionConCancelacionProgramada() {
        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        taller.setCancelacionProgramada(true);
        when(usuarioRepository.findByEmail("admin@tallertest.com")).thenReturn(Optional.of(usuario));

        suscripcionService.reactivarSuscripcion("admin@tallertest.com");

        assertFalse(taller.getCancelacionProgramada());
        verify(tallerRepository).save(taller);
    }

    @Test
    @DisplayName("Debe lanzar excepción si se intenta cancelar una suscripción ya suspendida")
    void debeLanzarExcepcionSiSuscripcionYaSuspendida() {
        taller.setEstadoSuscripcion(EstadoSuscripcion.SUSPENDIDA);
        when(usuarioRepository.findByEmail("admin@tallertest.com")).thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                suscripcionService.cancelarSuscripcion("admin@tallertest.com")
        );

        assertTrue(ex.getMessage().contains("ya se encuentra suspendida"));
        verify(tallerRepository, never()).save(taller);
    }
}
