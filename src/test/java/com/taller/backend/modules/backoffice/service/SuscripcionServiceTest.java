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
}
