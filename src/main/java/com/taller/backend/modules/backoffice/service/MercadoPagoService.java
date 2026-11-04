package com.taller.backend.modules.backoffice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

@Service
public class MercadoPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoService.class);
    private static final String MP_API_BASE = "https://api.mercadopago.com";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.webhook.secret:}")
    private String webhookSecret;

    @Value("${mercadopago.notification.url:}")
    private String notificationUrl;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public void setFrontendUrl(String frontendUrl) {
        this.frontendUrl = frontendUrl;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (!tieneAccessToken()) {
            String envToken = System.getenv("MP_ACCESS_TOKEN");
            if (envToken != null && !envToken.trim().isEmpty()) {
                this.accessToken = envToken.trim();
            }
        }
        if (!tieneAccessToken()) {
            try {
                java.nio.file.Path envPath = java.nio.file.Paths.get(".env");
                if (!java.nio.file.Files.exists(envPath)) {
                    envPath = java.nio.file.Paths.get("TallerMecanico_Backend/.env");
                }
                if (java.nio.file.Files.exists(envPath)) {
                    for (String linea : java.nio.file.Files.readAllLines(envPath)) {
                        linea = linea.trim();
                        if (linea.startsWith("MP_ACCESS_TOKEN=") || linea.startsWith("mercadopago.access-token=")) {
                            String val = linea.substring(linea.indexOf('=') + 1).trim();
                            if (!val.isEmpty()) {
                                this.accessToken = val;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (tieneAccessToken()) {
            log.info("MERCADO PAGO: Access Token cargado exitosamente.");
        } else {
            log.info("MERCADO PAGO: No se encontró Access Token. Operando en modo simulación.");
        }
    }

    public boolean tieneAccessToken() {
        return accessToken != null && !accessToken.trim().isEmpty();
    }

    /**
     * Crea una Preferencia de Pago en Mercado Pago.
     * Si 'mercadopago.access-token' está configurado, invoca el API oficial de Checkout Pro.
     * Si no está configurado (modo desarrollo/local), retorna la URL simulada.
     */
    public String crearPreferenciaPago(Taller taller, PlanSuscripcion plan) {
        if (!tieneAccessToken()) {
            log.info("MERCADO PAGO: 'mercadopago.access-token' no configurado. Utilizando modo simulado local.");
            String fakePreferenceId = UUID.randomUUID().toString();
            return (frontendUrl != null ? frontendUrl : "http://localhost:5173") + "/?mp_simulation=success&pref_id=" + fakePreferenceId + "&planId=" + plan.getId();
        }

        try {
            Map<String, Object> item = new HashMap<>();
            item.put("id", "plan_" + plan.getId());
            item.put("title", "PatitoFix - Plan " + plan.getTipo() + " (" + plan.getFrecuencia() + ")");
            item.put("description", "Suscripción al software para talleres mecánicos PatitoFix");
            item.put("quantity", 1);
            item.put("unit_price", plan.getPrecio());
            item.put("currency_id", "ARS");

            Map<String, Object> payer = new HashMap<>();
            payer.put("name", taller.getTitular() != null ? taller.getTitular() : taller.getNombre());
            payer.put("email", taller.getEmailContacto());

            String baseFront = frontendUrl != null ? frontendUrl : "http://localhost:5173";
            Map<String, String> backUrls = new HashMap<>();
            backUrls.put("success", baseFront + "/suscripcion");
            backUrls.put("failure", baseFront + "/suscripcion");
            backUrls.put("pending", baseFront + "/suscripcion");

            Map<String, Object> preferenceRequest = new HashMap<>();
            preferenceRequest.put("items", List.of(item));
            preferenceRequest.put("payer", payer);
            preferenceRequest.put("back_urls", backUrls);

            // Mercado Pago exige HTTPS para 'auto_return: approved'. En localhost no se debe incluir.
            if (baseFront.startsWith("https://")) {
                preferenceRequest.put("auto_return", "approved");
            }

            preferenceRequest.put("external_reference", "taller_" + taller.getId() + "_plan_" + plan.getId());

            if (notificationUrl != null && !notificationUrl.trim().isEmpty()) {
                preferenceRequest.put("notification_url", notificationUrl);
            }

            String requestBody = objectMapper.writeValueAsString(preferenceRequest);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MP_API_BASE + "/checkout/preferences"))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                boolean isTest = accessToken.trim().startsWith("TEST-");
                if (isTest && root.has("sandbox_init_point") && !root.get("sandbox_init_point").isNull()) {
                    return root.get("sandbox_init_point").asText();
                } else if (root.has("init_point") && !root.get("init_point").isNull()) {
                    return root.get("init_point").asText();
                }
            } else {
                JsonNode errJson = null;
                try { errJson = objectMapper.readTree(response.body()); } catch (Exception ignored) {}
                String errorMsg = (errJson != null && errJson.has("message")) ? errJson.get("message").asText() : response.body();
                log.error("Error al crear preferencia en Mercado Pago (status {}): {}", response.statusCode(), errorMsg);
                throw new BusinessRuleException("Mercado Pago (" + response.statusCode() + "): " + errorMsg);
            }

        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("Excepción al conectar con Mercado Pago: {}", e.getMessage(), e);
            throw new BusinessRuleException("Error al conectar con la pasarela de pagos: " + e.getMessage());
        }

        String fakePreferenceId = UUID.randomUUID().toString();
        return (frontendUrl != null ? frontendUrl : "http://localhost:5173") + "/?mp_simulation=success&pref_id=" + fakePreferenceId + "&planId=" + plan.getId();
    }

    /**
     * Consulta el estado de un pago en la API v1 de Mercado Pago.
     */
    public Map<String, Object> consultarPago(String paymentId) {
        if (!tieneAccessToken() || paymentId == null) {
            return null;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(MP_API_BASE + "/v1/payments/" + paymentId))
                    .header("Authorization", "Bearer " + accessToken.trim())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return objectMapper.readValue(response.body(), Map.class);
            } else {
                log.warn("Mercado Pago respondió status {} al consultar pago id {}", response.statusCode(), paymentId);
            }
        } catch (Exception e) {
            log.error("Error al consultar pago {} en Mercado Pago: {}", paymentId, e.getMessage());
        }

        return null;
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
