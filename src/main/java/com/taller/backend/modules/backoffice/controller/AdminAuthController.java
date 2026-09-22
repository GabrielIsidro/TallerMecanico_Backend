package com.taller.backend.modules.backoffice.controller;

import com.taller.backend.core.dto.AuthRequest;
import com.taller.backend.core.dto.AuthResponse;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.security.JwtUtil;
import com.taller.backend.modules.backoffice.model.SuperAdmin;
import com.taller.backend.modules.backoffice.repository.SuperAdminRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/backoffice/auth")
public class AdminAuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@Valid @RequestBody AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SuperAdmin admin = superAdminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con email: " + request.getEmail()));

        String jwt = jwtUtil.generateTokenAdmin(admin);
        return ResponseEntity.ok(new AuthResponse(jwt, "SUPERADMIN", null));
    }
}
