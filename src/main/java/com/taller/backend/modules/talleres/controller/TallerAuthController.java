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

@RestController
@RequestMapping("/api/v1/talleres/auth")
public class TallerAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    public ResponseEntity<?> loginTaller(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + request.getEmail()));

        String jwt = jwtUtil.generateTokenTaller(usuario);
        String tallerIdStr = usuario.getTaller() != null ? usuario.getTaller().getId().toString() : null;
        return ResponseEntity.ok(new AuthResponse(jwt, usuario.getRol().name(), tallerIdStr));
    }
}
