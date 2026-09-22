package com.taller.backend.modules.backoffice.service;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.FrecuenciaPlan;
import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.PlanSuscripcionRepository;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
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

        return mercadoPagoService.crearPreferenciaPago(taller, plan);
    }

    @Transactional
    public void procesarWebhook(Map<String, Object> payload, String authenticatedEmail) {
        String planIdStr = payload.get("planId") != null ? payload.get("planId").toString() : null;
        if (planIdStr == null) {
            throw new BusinessRuleException("El webhook no contiene el identificador de plan (planId).");
        }

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
    }

    @Transactional
    public void actualizarSuscripcionTaller(Long tallerId, Long planId) {
        PlanSuscripcion plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado con id: " + planId));

        Taller taller = tallerRepository.findById(tallerId)
                .orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con id: " + tallerId));

        taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);

        LocalDate fechaBase = (taller.getFechaVencimiento() != null && taller.getFechaVencimiento().isAfter(LocalDate.now()))
                ? taller.getFechaVencimiento()
                : LocalDate.now();

        taller.setTipoPlan(plan.getTipo());

        if (plan.getFrecuencia() == FrecuenciaPlan.ANUAL) {
            taller.setFechaVencimiento(fechaBase.plusYears(1));
        } else {
            taller.setFechaVencimiento(fechaBase.plusDays(30));
        }

        tallerRepository.save(taller);
    }

    public boolean validarFirmaWebhook(String xSignature, String xRequestId, String dataId) {
        return mercadoPagoService.validarFirmaWebhook(xSignature, xRequestId, dataId);
    }
}
