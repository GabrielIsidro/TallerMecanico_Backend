package com.taller.backend.modules.talleres.controller;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.DuplicateResourceException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.dto.ActualizarPerfilRequest;
import com.taller.backend.modules.talleres.dto.UsuarioResponseDTO;
import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/talleres/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityHelper securityHelper;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<UsuarioResponseDTO> obtenerMiPerfil() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        UsuarioResponseDTO response = new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getRol(),
                usuario.getTaller() != null ? usuario.getTaller().getId() : null,
                usuario.getTaller() != null ? usuario.getTaller().getNombre() : null,
                usuario.getTaller() != null ? usuario.getTaller().getEstadoSuscripcion() : null,
                usuario.getTaller() != null ? usuario.getTaller().getTipoPlan() : null,
                usuario.getTaller() != null ? usuario.getTaller().getFechaVencimiento() : null
        );
        response.setDebeCambiarPassword(Boolean.TRUE.equals(usuario.getDebeCambiarPassword()));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/actualizar-perfil")
    public ResponseEntity<?> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());

        if (request.getPasswordNueva() != null && !request.getPasswordNueva().trim().isEmpty()) {
            if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                throw new BusinessRuleException("La contraseña actual es incorrecta. No se pudo actualizar la clave.");
            }
            usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
            usuario.setDebeCambiarPassword(false);
        }

        usuarioRepository.save(usuario);

        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "¡Perfil actualizado con éxito!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/equipo")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarEquipo() {
        Taller taller = securityHelper.getTallerAutenticado();
        List<Usuario> equipo = usuarioRepository.findByTallerIdAndRol(taller.getId(), RolUsuario.MECANICO);
        
        List<UsuarioResponseDTO> response = equipo.stream()
            .map(u -> new UsuarioResponseDTO(
                u.getId(), u.getEmail(), u.getNombre(), u.getApellido(), u.getRol(),
                taller.getId(), taller.getNombre(), taller.getEstadoSuscripcion(), taller.getTipoPlan(), taller.getFechaVencimiento()
            )).collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }

    @PostMapping("/equipo")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> crearMiembroEquipo(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String nombre = request.get("nombre");
        String apellido = request.get("apellido");
        String password = request.get("password");

        if (email == null || email.trim().isEmpty()) {
            throw new BusinessRuleException("El correo electrónico es obligatorio.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new BusinessRuleException("La contraseña es obligatoria.");
        }

        if (usuarioRepository.findByEmail(email.trim()).isPresent()) {
            throw new DuplicateResourceException("El correo ya está registrado.");
        }

        Taller taller = securityHelper.getTallerAutenticado();

        Usuario mecanico = new Usuario();
        mecanico.setEmail(email.trim());
        mecanico.setNombre(nombre);
        mecanico.setApellido(apellido);
        mecanico.setPassword(passwordEncoder.encode(password.trim()));
        mecanico.setRol(RolUsuario.MECANICO);
        mecanico.setTaller(taller);

        usuarioRepository.save(mecanico);

        return ResponseEntity.ok(new UsuarioResponseDTO(
            mecanico.getId(), mecanico.getEmail(), mecanico.getNombre(), mecanico.getApellido(), mecanico.getRol(),
            taller.getId(), taller.getNombre(), taller.getEstadoSuscripcion(), taller.getTipoPlan(), taller.getFechaVencimiento()
        ));
    }

    @DeleteMapping("/equipo/{id}")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> eliminarMiembroEquipo(@PathVariable Long id) {
        Taller taller = securityHelper.getTallerAutenticado();
        
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));

        if (usuario.getTaller() == null || !usuario.getTaller().getId().equals(taller.getId()) || !usuario.getRol().equals(RolUsuario.MECANICO)) {
            throw new UnauthorizedAccessException("Acceso denegado o usuario inválido");
        }
        usuarioRepository.delete(usuario);
        return ResponseEntity.ok().build();
    }
}
