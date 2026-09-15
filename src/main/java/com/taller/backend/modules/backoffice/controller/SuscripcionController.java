package com.taller.backend.modules.backoffice.controller;

import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.model.TipoPlan;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backoffice/suscripciones")
@CrossOrigin(origins = "*")
public class SuscripcionController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private com.taller.backend.modules.backoffice.service.MercadoPagoService mercadoPagoService;

    @Autowired
    private com.taller.backend.modules.backoffice.repository.PlanSuscripcionRepository planRepository;

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> generarCheckoutUrl(@RequestBody Map<String, String> request) {
        String planIdStr = request.get("planId");
        if (planIdStr == null) {
            return ResponseEntity.badRequest().body("Debe enviar el planId");
        }
        
        Long planId = Long.parseLong(planIdStr);
        com.taller.backend.modules.backoffice.model.PlanSuscripcion planSeleccionado = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Taller taller = usuario.getTaller();
        if (taller == null) {
            return ResponseEntity.badRequest().body("El usuario no pertenece a ningún taller.");
        }

        // Llamamos al servicio de MercadoPago para obtener el link
        String checkoutUrl = mercadoPagoService.crearPreferenciaPago(taller, planSeleccionado);

        Map<String, String> response = new HashMap<>();
        response.put("checkoutUrl", checkoutUrl);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> recibirWebhookMP(@RequestBody Map<String, Object> payload) {
        System.out.println("WEBHOOK RECIBIDO: " + payload);
        
        String planIdStr = payload.get("planId") != null ? payload.get("planId").toString() : null;
        if (planIdStr == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long planId = Long.parseLong(planIdStr);
        
        // Simulación de acreditación:
        // Buscamos un taller (en caso real, usamos metadata de MP, por ahora obtenemos el del usuario logueado o si es mock el ID 1)
        // Para que la simulación funcione para el usuario logueado en la demo local:
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        if ("anonymousUser".equals(email)) {
            // Si el webhook viene sin Auth (ej. de MercadoPago real), forzamos taller 1 por ser demo
            actualizarSuscripcionTaller(1L, planId);
        } else {
            // Si vino del frontend simulado con Auth, actualizamos el del usuario actual
            usuarioRepository.findByEmail(email).ifPresent(usuario -> {
                if (usuario.getTaller() != null) {
                    actualizarSuscripcionTaller(usuario.getTaller().getId(), planId);
                }
            });
        }

        return ResponseEntity.ok().build();
    }
    
    private void actualizarSuscripcionTaller(Long tallerId, Long planId) {
        planRepository.findById(planId).ifPresent(plan -> {
            tallerRepository.findById(tallerId).ifPresent(taller -> {
                taller.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
                
                LocalDate fechaBase = (taller.getFechaVencimiento() != null && taller.getFechaVencimiento().isAfter(LocalDate.now()))
                        ? taller.getFechaVencimiento()
                        : LocalDate.now();
                
                taller.setTipoPlan(plan.getTipo());
                
                if (plan.getFrecuencia() == com.taller.backend.modules.backoffice.model.FrecuenciaPlan.ANUAL) {
                    taller.setFechaVencimiento(fechaBase.plusYears(1));
                } else {
                    taller.setFechaVencimiento(fechaBase.plusDays(30));
                }
                
                tallerRepository.save(taller);
                System.out.println("Taller renovado por Webhook. Plan ID: " + planId);
            });
        });
    }
}
