package com.taller.backend.core.security;

import com.taller.backend.core.exception.PaymentRequiredException;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuscripcionInterceptorTest {

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private SuscripcionInterceptor interceptor;

    private Taller taller;

    @BeforeEach
    void setUp() {
        taller = new Taller();
        taller.setId(1L);
        taller.setNombre("Taller Test");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN_TALLER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testPermiteOptionsPreflight() throws Exception {
        when(request.getMethod()).thenReturn("OPTIONS");
        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void testPermiteSuperAdminSinRestriccion() throws Exception {
        UsernamePasswordAuthenticationToken superAdminAuth = new UsernamePasswordAuthenticationToken(
                "super@test.com", "pass", List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(superAdminAuth);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/talleres/clientes");

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void testPermiteTodoCuandoActivaOPrueba() throws Exception {
        taller.setEstadoSuscripcion(EstadoSuscripcion.PRUEBA_GRATUITA);
        when(securityHelper.getTallerAutenticado()).thenReturn(taller);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/talleres/clientes");

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void testVencidaPermiteGetLectura() throws Exception {
        taller.setEstadoSuscripcion(EstadoSuscripcion.VENCIDA);
        when(securityHelper.getTallerAutenticado()).thenReturn(taller);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/talleres/clientes");

        boolean result = interceptor.preHandle(request, response, new Object());
        assertTrue(result);
    }

    @Test
    void testVencidaBloqueaPostEscritura() {
        taller.setEstadoSuscripcion(EstadoSuscripcion.VENCIDA);
        when(securityHelper.getTallerAutenticado()).thenReturn(taller);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/talleres/clientes");

        assertThrows(PaymentRequiredException.class, () ->
                interceptor.preHandle(request, response, new Object())
        );
    }

    @Test
    void testSuspendidaBloqueaTodo() {
        taller.setEstadoSuscripcion(EstadoSuscripcion.SUSPENDIDA);
        when(securityHelper.getTallerAutenticado()).thenReturn(taller);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/talleres/clientes");

        assertThrows(PaymentRequiredException.class, () ->
                interceptor.preHandle(request, response, new Object())
        );
    }
}
