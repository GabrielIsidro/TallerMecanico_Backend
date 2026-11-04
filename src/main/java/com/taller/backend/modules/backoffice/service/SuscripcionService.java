package com.taller.backend.modules.backoffice.service;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.FrecuenciaPlan;
import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.PlanSuscripcionRepository;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
public class SuscripcionService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private PlanSuscripcionRepository planRepository;

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @Transactional(readOnly = true)
    public String generarCheckoutUrl(Long planId, String email) {
        PlanSuscripcion plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan de suscripción no encontrado con id: " + planId));

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        Taller taller = usuario.getTaller();
        if (taller == null) {
            throw new BusinessRuleException("El usuario autenticado no pertenece a ningún taller.");
        }

        // Validación: evitar cobros dobles si ya posee el mismo plan activo con vigencia mayor a 7 días
        if (taller.getTipoPlan() == plan.getTipo() && taller.getEstadoSuscripcion() == EstadoSuscripcion.ACTIVA) {
            if (taller.getFechaVencimiento() != null && taller.getFechaVencimiento().isAfter(LocalDate.now())) {
                long diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), taller.getFechaVencimiento());
                if (diasRestantes > 7) {
                    throw new BusinessRuleException("Tu plan " + plan.getTipo() + " ya se encuentra activo (" + diasRestantes + " días restantes). Podrás renovarlo cuando falten 7 días o menos para su vencimiento.");
                }
            }
        }

        return mercadoPagoService.crearPreferenciaPago(taller, plan);
    }

    @Transactional
    public void procesarWebhook(Map<String, Object> payload, String authenticatedEmail) {
        String dataId = null;
        if (payload.get("data") instanceof Map<?, ?> dataMap && dataMap.get("id") != null) {
            dataId = dataMap.get("id").toString();
        } else if (payload.get("id") != null) {
            dataId = payload.get("id").toString();
        }

        // Si Mercado Pago envió una notificación real de pago con access token activo:
        if (dataId != null && !"simulado".equals(dataId) && mercadoPagoService.tieneAccessToken()) {
            Map<String, Object> pagoData = mercadoPagoService.consultarPago(dataId);
            if (pagoData != null) {
                String status = (String) pagoData.get("status");
                String extRef = (String) pagoData.get("external_reference");
                if ("approved".equalsIgnoreCase(status) && extRef != null) {
                    String[] parts = extRef.split("_");
                    Long tallerId = null;
                    Long planId = null;
                    for (int i = 0; i < parts.length - 1; i++) {
                        if ("taller".equalsIgnoreCase(parts[i])) {
                            try { tallerId = Long.parseLong(parts[i + 1]); } catch (NumberFormatException ignored) {}
                        } else if ("plan".equalsIgnoreCase(parts[i])) {
                            try { planId = Long.parseLong(parts[i + 1]); } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (tallerId != null && planId != null) {
                        actualizarSuscripcionTaller(tallerId, planId);
                        return;
                    }
                }
            }
        }

        // Fallback para simulación local / desarrollo
        String planIdStr = payload.get("planId") != null ? payload.get("planId").toString() : null;
        if (planIdStr != null) {
            Long planId = Long.parseLong(planIdStr);
            Long tallerId = null;

            if (payload.get("tallerId") != null) {
                try {
                    tallerId = Long.parseLong(payload.get("tallerId").toString());
                } catch (NumberFormatException ignored) {}
            }

            if (tallerId == null) {
                if (authenticatedEmail != null && !"anonymousUser".equals(authenticatedEmail)) {
                    Usuario usuario = usuarioRepository.findByEmail(authenticatedEmail).orElse(null);
                    if (usuario != null && usuario.getTaller() != null) {
                        tallerId = usuario.getTaller().getId();
                    }
                }
                if (tallerId == null) {
                    // Fallback para pruebas / simulación local
                    tallerId = 1L;
                }
            }

            actualizarSuscripcionTaller(tallerId, planId);
            return;
        }

        throw new BusinessRuleException("El payload del webhook debe incluir 'planId' o información válida de Mercado Pago.");
    }

    @Transactional
    public void actualizarSuscripcionTaller(Long tallerId, Long planId) {
        PlanSuscripcion plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado con id: " + planId));

        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con id: " + tallerId));

        boolean esMismoPlan = taller.getTipoPlan() == plan.getTipo()
                && taller.getEstadoSuscripcion() == EstadoSuscripcion.ACTIVA;

        LocalDate fechaBase;
        // Solo acumulamos días si es una renovación del MISMO plan activo y aún no venció
        if (esMismoPlan && taller.getFechaVencimiento() != null && taller.getFechaVencimiento().isAfter(LocalDate.now())) {
            fechaBase = taller.getFechaVencimiento();
        } else {
            // Cambio de plan (ej. BASE a PRO, o de PRUEBA_GRATUITA a pago, o suscripción vencida):
            // La vigencia rige a partir de HOY, sin arrastrar días del plan anterior
            fechaBase = LocalDate.now();
        }

        taller.setTipoPlan(plan.getTipo());
        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        taller.setCancelacionProgramada(false);

        if (plan.getFrecuencia() == FrecuenciaPlan.ANUAL) {
            taller.setFechaVencimiento(fechaBase.plusYears(1));
        } else {
            taller.setFechaVencimiento(fechaBase.plusDays(30));
        }

        tallerRepository.save(taller);
    }

    @Transactional
    public Taller confirmarPago(String paymentId, String authenticatedEmail) {
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new BusinessRuleException("Debe proporcionar el número de operación / pago de Mercado Pago.");
        }

        if (!mercadoPagoService.tieneAccessToken()) {
            throw new BusinessRuleException("Mercado Pago no tiene credenciales configuradas.");
        }

        Map<String, Object> pagoData = mercadoPagoService.consultarPago(paymentId.trim());
        if (pagoData == null) {
            throw new BusinessRuleException("No se encontró el pago N.° " + paymentId + " en Mercado Pago.");
        }

        String status = (String) pagoData.get("status");
        if (!"approved".equalsIgnoreCase(status)) {
            String statusDetail = (String) pagoData.get("status_detail");
            throw new BusinessRuleException("El pago N.° " + paymentId + " no se encuentra aprobado. Estado actual: " + status + (statusDetail != null ? " (" + statusDetail + ")" : ""));
        }

        String extRef = (String) pagoData.get("external_reference");
        if (extRef == null || extRef.trim().isEmpty()) {
            throw new BusinessRuleException("El pago no contiene referencia del taller o plan asociado.");
        }

        String[] parts = extRef.split("_");
        Long tallerId = null;
        Long planId = null;
        for (int i = 0; i < parts.length - 1; i++) {
            if ("taller".equalsIgnoreCase(parts[i])) {
                try { tallerId = Long.parseLong(parts[i + 1]); } catch (NumberFormatException ignored) {}
            } else if ("plan".equalsIgnoreCase(parts[i])) {
                try { planId = Long.parseLong(parts[i + 1]); } catch (NumberFormatException ignored) {}
            }
        }

        if (tallerId == null || planId == null) {
            throw new BusinessRuleException("Referencia de pago no válida para PatitoFix: " + extRef);
        }

        // Validación de pertenencia del taller al usuario autenticado (seguridad multi-tenant)
        if (authenticatedEmail != null && !"anonymousUser".equals(authenticatedEmail)) {
            Usuario usuario = usuarioRepository.findByEmail(authenticatedEmail).orElse(null);
            if (usuario != null && usuario.getTaller() != null) {
                if (!usuario.getTaller().getId().equals(tallerId) && usuario.getRol() != RolUsuario.SUPER_ADMIN) {
                    throw new BusinessRuleException("El pago N.° " + paymentId + " no corresponde a tu taller.");
                }
            }
        }

        actualizarSuscripcionTaller(tallerId, planId);
        final Long finalTallerId = tallerId;
        return tallerRepository.findById(finalTallerId).orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con id: " + finalTallerId));
    }

    @Transactional
    public void cancelarSuscripcion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        Taller taller = usuario.getTaller();
        if (taller == null) {
            throw new BusinessRuleException("El usuario autenticado no pertenece a ningún taller.");
        }

        if (taller.getEstadoSuscripcion() == EstadoSuscripcion.SUSPENDIDA) {
            throw new BusinessRuleException("La suscripción del taller ya se encuentra suspendida.");
        }

        if (Boolean.TRUE.equals(taller.getCancelacionProgramada())) {
            throw new BusinessRuleException("La suscripción ya tiene una cancelación programada para el " + taller.getFechaVencimiento() + ".");
        }

        // Programar cancelación para el fin del período: el taller sigue operando normalmente hasta la fecha de vencimiento
        taller.setCancelacionProgramada(true);
        tallerRepository.save(taller);
    }

    @Transactional
    public void reactivarSuscripcion(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        Taller taller = usuario.getTaller();
        if (taller == null) {
            throw new BusinessRuleException("El usuario autenticado no pertenece a ningún taller.");
        }

        if (!Boolean.TRUE.equals(taller.getCancelacionProgramada())) {
            throw new BusinessRuleException("La suscripción no tiene una cancelación programada pendiente.");
        }

        taller.setCancelacionProgramada(false);
        tallerRepository.save(taller);
    }

    public boolean validarFirmaWebhook(String xSignature, String xRequestId, String dataId) {
        return mercadoPagoService.validarFirmaWebhook(xSignature, xRequestId, dataId);
    }
}
