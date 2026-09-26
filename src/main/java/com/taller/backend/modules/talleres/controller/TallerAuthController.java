package com.taller.backend.modules.talleres.controller;

import com.taller.backend.core.dto.AuthRequest;
import com.taller.backend.core.dto.AuthResponse;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.security.JwtUtil;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.service.EmailService;
import com.taller.backend.modules.talleres.dto.ConfirmarRecuperacionDTO;
import com.taller.backend.modules.talleres.dto.SolicitarRecuperacionDTO;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/talleres/auth")
public class TallerAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> loginTaller(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + request.getEmail()));

        String jwt = jwtUtil.generateTokenTaller(usuario);
        String tallerIdStr = usuario.getTaller() != null ? usuario.getTaller().getId().toString() : null;
        boolean debeCambiar = Boolean.TRUE.equals(usuario.getDebeCambiarPassword());
        String estadoSuscripcion = usuario.getTaller() != null && usuario.getTaller().getEstadoSuscripcion() != null
                ? usuario.getTaller().getEstadoSuscripcion().name()
                : null;
        return ResponseEntity.ok(new AuthResponse(jwt, usuario.getRol().name(), tallerIdStr, debeCambiar, estadoSuscripcion));
    }

    @PostMapping("/recuperar-password/solicitar")
    public ResponseEntity<?> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionDTO request) {
        String email = request.getEmail().trim();
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        Map<String, String> response = new HashMap<>();
        if (usuarioOpt.isEmpty()) {
            response.put("mensaje", "Si el correo está registrado en la plataforma, recibirás un código de verificación en breve.");
            return ResponseEntity.ok(response);
        }

        Usuario usuario = usuarioOpt.get();
        int codigoNum = 100000 + new java.security.SecureRandom().nextInt(900000);
        String codigo = String.valueOf(codigoNum);

        usuario.setCodigoRecuperacion(codigo);
        usuario.setCodigoRecuperacionExpiracion(LocalDateTime.now().plusMinutes(15));
        usuarioRepository.save(usuario);

        emailService.enviarCodigoRecuperacion(usuario.getEmail(), codigo);

        response.put("mensaje", "Código de verificación enviado con éxito.");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recuperar-password/confirmar")
    public ResponseEntity<?> confirmarRecuperacion(@Valid @RequestBody ConfirmarRecuperacionDTO request) {
        String email = request.getEmail().trim();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessRuleException("El correo ingresado no corresponde a ninguna cuenta activa."));

        if (usuario.getCodigoRecuperacion() == null || usuario.getCodigoRecuperacionExpiracion() == null) {
            throw new BusinessRuleException("No hay ninguna solicitud de recuperación pendiente para esta cuenta o ya fue utilizada.");
        }

        if (usuario.getCodigoRecuperacionExpiracion().isBefore(LocalDateTime.now())) {
            usuario.setCodigoRecuperacion(null);
            usuario.setCodigoRecuperacionExpiracion(null);
            usuarioRepository.save(usuario);
            throw new BusinessRuleException("El código de verificación ha expirado. Por favor, solicita uno nuevo.");
        }

        if (!usuario.getCodigoRecuperacion().trim().equals(request.getCodigo().trim())) {
            throw new BusinessRuleException("El código de verificación ingresado es incorrecto.");
        }

        if (!request.getNuevaPassword().equals(request.getConfirmarPassword())) {
            throw new BusinessRuleException("Las contraseñas no coinciden.");
        }

        if (request.getNuevaPassword().trim().length() < 6) {
            throw new BusinessRuleException("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword().trim()));
        usuario.setCodigoRecuperacion(null);
        usuario.setCodigoRecuperacionExpiracion(null);
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "¡Contraseña restablecida con éxito! Ya puedes iniciar sesión con tu nueva clave.");
        return ResponseEntity.ok(response);
    }
}
