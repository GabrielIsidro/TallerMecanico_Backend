package com.taller.backend.modules.backoffice.service;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);

    @Value("${mercadopago.webhook.secret:}")
    private String webhookSecret;

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    /**
     * Simula la creación de una Preferencia de Pago en MercadoPago.
     * En producción, aquí se usaría el SDK de MP con el ACCESS_TOKEN.
     */
    public String crearPreferenciaPago(Taller taller, PlanSuscripcion plan) {
        // 1. Aquí se validaría el monto según el plan
        double monto = plan.getPrecio();
        
        // 2. Aquí llamaríamos a MercadoPago API para obtener el preferenceId
        String fakePreferenceId = UUID.randomUUID().toString();
        
        // 3. Devolvemos una URL de checkout.
        return "http://localhost:5173/?mp_simulation=success&pref_id=" + fakePreferenceId + "&planId=" + plan.getId();
    }

    /**
     * Valida criptográficamente la cabecera x-signature de Mercado Pago usando HMAC-SHA256.
     * Si no hay secreto configurado (modo desarrollo), permite la ejecución registrando una advertencia.
     */
    public boolean validarFirmaWebhook(String xSignature, String xRequestId, String dataId) {
        if (webhookSecret == null || webhookSecret.trim().isEmpty()) {
            log.warn("MERCADO PAGO WEBHOOK: 'mercadopago.webhook.secret' no está configurado. Omitiendo validación criptográfica (Modo Desarrollo/Local).");
            return true;
        }

        if (xSignature == null || xSignature.trim().isEmpty()) {
            log.warn("MERCADO PAGO WEBHOOK: Cabecera 'x-signature' ausente.");
            return false;
        }

        String ts = null;
        String hashV1 = null;

        for (String part : xSignature.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                if ("ts".equalsIgnoreCase(kv[0])) {
                    ts = kv[1];
                } else if ("v1".equalsIgnoreCase(kv[0])) {
                    hashV1 = kv[1];
                }
            }
        }

        if (ts == null || hashV1 == null) {
            log.warn("MERCADO PAGO WEBHOOK: Formato de 'x-signature' inválido. Se requieren partes 'ts' y 'v1'.");
            return false;
        }

        // Manifiesto oficial Mercado Pago: id:[data.id];request-id:[x-request-id];ts:[ts];
        String manifest = "id:" + (dataId != null ? dataId : "")
                + ";request-id:" + (xRequestId != null ? xRequestId : "")
                + ";ts:" + ts + ";";

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computedBytes = mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(computedBytes);

            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    hashV1.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new BusinessRuleException("Fallo al calcular firma criptográfica HMAC-SHA256: " + e.getMessage());
        }
    }
}
