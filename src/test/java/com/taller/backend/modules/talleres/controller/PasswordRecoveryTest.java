package com.taller.backend.modules.talleres.controller;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.service.EmailService;
import com.taller.backend.modules.talleres.dto.ConfirmarRecuperacionDTO;
import com.taller.backend.modules.talleres.dto.SolicitarRecuperacionDTO;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordRecoveryTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TallerAuthController tallerAuthController;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("taller@test.com");
        usuario.setPassword("oldEncodedPassword");
        usuario.setDebeCambiarPassword(true);
    }

    @Test
    @DisplayName("Debe generar código de 6 dígitos y enviar email si el correo existe")
    void debeGenerarCodigoYEnviarEmail() {
        SolicitarRecuperacionDTO dto = new SolicitarRecuperacionDTO("taller@test.com");
        when(usuarioRepository.findByEmail("taller@test.com")).thenReturn(Optional.of(usuario));

        ResponseEntity<?> response = tallerAuthController.solicitarRecuperacion(dto);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(usuario.getCodigoRecuperacion());
        assertEquals(6, usuario.getCodigoRecuperacion().length());
        assertTrue(usuario.getCodigoRecuperacionExpiracion().isAfter(LocalDateTime.now()));
        verify(emailService, times(1)).enviarCodigoRecuperacion(eq("taller@test.com"), anyString());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("Debe responder amistosamente sin error si el correo no existe")
    void debeResponderAmistosamenteSiCorreoNoExiste() {
        SolicitarRecuperacionDTO dto = new SolicitarRecuperacionDTO("noexiste@test.com");
        when(usuarioRepository.findByEmail("noexiste@test.com")).thenReturn(Optional.empty());

        ResponseEntity<?> response = tallerAuthController.solicitarRecuperacion(dto);

        assertEquals(200, response.getStatusCode().value());
        verify(emailService, never()).enviarCodigoRecuperacion(anyString(), anyString());
    }

    @Test
    @DisplayName("Debe confirmar recuperación exitosamente si el código es correcto y vigente")
    void debeConfirmarRecuperacionExitosamente() {
        usuario.setCodigoRecuperacion("123456");
        usuario.setCodigoRecuperacionExpiracion(LocalDateTime.now().plusMinutes(10));

        ConfirmarRecuperacionDTO dto = new ConfirmarRecuperacionDTO(
                "taller@test.com", "123456", "NuevaClave123", "NuevaClave123"
        );

        when(usuarioRepository.findByEmail("taller@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("NuevaClave123")).thenReturn("newEncodedPassword");

        ResponseEntity<?> response = tallerAuthController.confirmarRecuperacion(dto);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("newEncodedPassword", usuario.getPassword());
        assertNull(usuario.getCodigoRecuperacion());
        assertNull(usuario.getCodigoRecuperacionExpiracion());
        assertFalse(usuario.getDebeCambiarPassword());
        verify(usuarioRepository, times(1)).save(usuario);
    }

    @Test
    @DisplayName("Debe rechazar la confirmación si el código está expirado")
    void debeRechazarSiCodigoEstaExpirado() {
        usuario.setCodigoRecuperacion("123456");
        usuario.setCodigoRecuperacionExpiracion(LocalDateTime.now().minusMinutes(1));

        ConfirmarRecuperacionDTO dto = new ConfirmarRecuperacionDTO(
                "taller@test.com", "123456", "NuevaClave123", "NuevaClave123"
        );

        when(usuarioRepository.findByEmail("taller@test.com")).thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                tallerAuthController.confirmarRecuperacion(dto)
        );

        assertTrue(ex.getMessage().contains("expirado"));
    }

    @Test
    @DisplayName("Debe rechazar la confirmación si el código no coincide")
    void debeRechazarSiCodigoNoCoincide() {
        usuario.setCodigoRecuperacion("654321");
        usuario.setCodigoRecuperacionExpiracion(LocalDateTime.now().plusMinutes(10));

        ConfirmarRecuperacionDTO dto = new ConfirmarRecuperacionDTO(
                "taller@test.com", "123456", "NuevaClave123", "NuevaClave123"
        );

        when(usuarioRepository.findByEmail("taller@test.com")).thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                tallerAuthController.confirmarRecuperacion(dto)
        );

        assertTrue(ex.getMessage().contains("incorrecto"));
    }

    @Test
    @DisplayName("Debe rechazar la confirmación si las contraseñas no coinciden")
    void debeRechazarSiPasswordsNoCoinciden() {
        usuario.setCodigoRecuperacion("123456");
        usuario.setCodigoRecuperacionExpiracion(LocalDateTime.now().plusMinutes(10));

        ConfirmarRecuperacionDTO dto = new ConfirmarRecuperacionDTO(
                "taller@test.com", "123456", "NuevaClave123", "OtraClave999"
        );

        when(usuarioRepository.findByEmail("taller@test.com")).thenReturn(Optional.of(usuario));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                tallerAuthController.confirmarRecuperacion(dto)
        );

        assertTrue(ex.getMessage().contains("no coinciden"));
    }
}
