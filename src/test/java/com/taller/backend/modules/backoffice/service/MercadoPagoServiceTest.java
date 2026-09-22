package com.taller.backend.modules.backoffice.service;

import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.model.TipoPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoServiceTest {

    private MercadoPagoService mercadoPagoService;

    @BeforeEach
    void setUp() {
        mercadoPagoService = new MercadoPagoService();
    }

    @Test
    @DisplayName("Debe permitir webhook si el secreto no está configurado (modo desarrollo)")
    void debePermitirWebhookSinSecretoConfiguradoModoDesarrollo() {
        mercadoPagoService.setWebhookSecret("");
        boolean resultado = mercadoPagoService.validarFirmaWebhook(null, null, "12345");
        assertTrue(resultado, "Debe ser true en modo desarrollo cuando el secreto es nulo o vacío");
    }

    @Test
    @DisplayName("Debe validar exitosamente firma criptográfica legítima de Mercado Pago")
    void debeValidarFirmaCorrectaHmacSha256() throws Exception {
        String secret = "mi_secreto_super_seguro_mp_123";
        mercadoPagoService.setWebhookSecret(secret);

        String dataId = "123456789";
        String requestId = "req-uuid-test";
        String ts = "1710960000";

        // Plantilla oficial: id:[data.id];request-id:[x-request-id];ts:[ts];
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String validHash = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));

        String xSignature = "ts=" + ts + ",v1=" + validHash;

        boolean resultado = mercadoPagoService.validarFirmaWebhook(xSignature, requestId, dataId);
        assertTrue(resultado, "La firma calculada debe coincidir con la cabecera v1");
    }

    @Test
    @DisplayName("Debe rechazar firma falsificada o alterada")
    void debeRechazarFirmaFalsificadaOAlterada() {
        mercadoPagoService.setWebhookSecret("mi_secreto_mp");

        String xSignature = "ts=1710960000,v1=hashfalsificadoinvalido";
        boolean resultado = mercadoPagoService.validarFirmaWebhook(xSignature, "req-1", "12345");
        assertFalse(resultado, "Debe rechazar una firma que no coincide con el HMAC esperado");
    }

    @Test
    @DisplayName("Debe rechazar webhook si falta la cabecera de firma cuando el secreto está activo")
    void debeRechazarWebhookSiFaltaCabecera() {
        mercadoPagoService.setWebhookSecret("mi_secreto_mp");

        assertFalse(mercadoPagoService.validarFirmaWebhook(null, "req-1", "12345"));
        assertFalse(mercadoPagoService.validarFirmaWebhook("", "req-1", "12345"));
        assertFalse(mercadoPagoService.validarFirmaWebhook("ts=1710960000", "req-1", "12345")); // falta v1
    }

    @Test
    @DisplayName("Debe generar preferencia de pago simulada correctamente")
    void debeGenerarPreferenciaPagoSimulada() {
        Taller taller = new Taller();
        taller.setId(1L);
        taller.setNombre("Taller Central");

        PlanSuscripcion plan = new PlanSuscripcion();
        plan.setId(2L);
        plan.setDescripcion("Plan Profesional");
        plan.setTipo(TipoPlan.PRO);
        plan.setPrecio(15000.0);

        String url = mercadoPagoService.crearPreferenciaPago(taller, plan);

        assertNotNull(url);
        assertTrue(url.contains("planId=2"));
        assertTrue(url.contains("mp_simulation=success"));
    }
}
