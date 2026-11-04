package com.taller.backend.modules.backoffice.controller;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.service.SuscripcionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backoffice/suscripciones")
@CrossOrigin(origins = "*")
public class SuscripcionController {

    @Autowired
    private SuscripcionService suscripcionService;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> generarCheckoutUrl(@RequestBody Map<String, String> request) {
        String planIdStr = request.get("planId");
        if (planIdStr == null || planIdStr.trim().isEmpty()) {
            throw new BusinessRuleException("Debe enviar el planId para iniciar el checkout.");
        }

        Long planId = Long.parseLong(planIdStr.trim());
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        String checkoutUrl = suscripcionService.generarCheckoutUrl(planId, email);

        Map<String, String> response = new HashMap<>();
        response.put("checkoutUrl", checkoutUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> recibirWebhookMP(
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestParam(value = "data.id", required = false) String dataIdParam,
            @RequestBody(required = false) Map<String, Object> payload) {

        String dataId = dataIdParam;
        if (dataId == null && payload != null) {
            Object dataObj = payload.get("data");
            if (dataObj instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
                dataId = dataMap.get("id").toString();
            } else if (payload.get("id") != null) {
                dataId = payload.get("id").toString();
            }
        }

        boolean isValidSignature = suscripcionService.validarFirmaWebhook(xSignature, xRequestId, dataId);
        if (!isValidSignature) {
            throw new UnauthorizedAccessException("Firma criptográfica de webhook de Mercado Pago ausente o inválida.");
        }

        String email = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;

        suscripcionService.procesarWebhook(payload != null ? payload : Map.of(), email);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirmar-pago")
    @PreAuthorize("hasRole('ADMIN_TALLER') or hasRole('SUPERADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> confirmarPago(@RequestBody Map<String, String> request) {
        String paymentId = request.get("paymentId");
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new BusinessRuleException("Debe enviar el paymentId para confirmar el pago.");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Taller taller = suscripcionService.confirmarPago(paymentId.trim(), email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "¡Pago verificado con éxito en Mercado Pago! Tu suscripción ha sido activada.");
        response.put("tallerId", taller.getId());
        response.put("tipoPlan", taller.getTipoPlan());
        response.put("estadoSuscripcion", taller.getEstadoSuscripcion());
        response.put("fechaVencimiento", taller.getFechaVencimiento());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/cancelar")
    @PreAuthorize("hasRole('ADMIN_TALLER') or hasRole('SUPERADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> cancelarSuscripcion() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        suscripcionService.cancelarSuscripcion(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cancelación programada: mantendrás el acceso a tu plan hasta el fin del período.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reactivar")
    @PreAuthorize("hasRole('ADMIN_TALLER') or hasRole('SUPERADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> reactivarSuscripcion() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        suscripcionService.reactivarSuscripcion(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cancelación anulada. Tu suscripción continuará activa normalmente.");
        return ResponseEntity.ok(response);
    }
}
