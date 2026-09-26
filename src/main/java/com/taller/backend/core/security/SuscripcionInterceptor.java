package com.taller.backend.core.security;

import com.taller.backend.core.exception.PaymentRequiredException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SuscripcionInterceptor implements HandlerInterceptor {

    @Autowired
    private SecurityHelper securityHelper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Permitir preflight CORS sin validar suscripción
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();

        // Excluir endpoints públicos o de consulta del propio usuario / auth
        if (uri.contains("/talleres/auth") || uri.endsWith("/usuarios/me")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true; // Si no está autenticado, SecurityConfig se encargará de rechazarlo
        }

        // Si es SUPERADMIN o SUPER_ADMIN, nunca se bloquea
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("SUPERADMIN") || a.getAuthority().contains("SUPER_ADMIN"));
        if (isSuperAdmin) {
            return true;
        }

        try {
            Taller taller = securityHelper.getTallerAutenticado();
            if (taller == null || taller.getEstadoSuscripcion() == null) {
                return true;
            }

            EstadoSuscripcion estado = taller.getEstadoSuscripcion();
            String method = request.getMethod();

            if (estado == EstadoSuscripcion.SUSPENDIDA) {
                throw new PaymentRequiredException("Su taller se encuentra suspendido por falta de pago. Regularice su suscripción para operar.");
            }

            if (estado == EstadoSuscripcion.VENCIDA) {
                // Modo Solo Lectura en Gracia: se permiten únicamente métodos GET de lectura
                if (!"GET".equalsIgnoreCase(method)) {
                    throw new PaymentRequiredException("Su suscripción ha vencido y se encuentra en período de gracia de solo lectura. Regularice su plan para crear o modificar datos.");
                }
            }

        } catch (ResourceNotFoundException | UnauthorizedAccessException e) {
            // Si no tiene taller asignado o no se encuentra, continuar flujo normal
            return true;
        }

        return true;
    }
}
