package com.taller.backend.controller;

import com.taller.backend.dto.ActualizarPerfilRequest; // <--- Cambiado el DTO
import com.taller.backend.model.Usuario;
import com.taller.backend.model.RolUsuario;
import com.taller.backend.model.Taller;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SecurityHelper securityHelper;

    // ---> NUEVO: Endpoint para obtener los datos del usuario logueado (para llenar el formulario)
    @GetMapping("/me")
    public ResponseEntity<com.taller.backend.dto.UsuarioResponseDTO> obtenerMiPerfil() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        com.taller.backend.dto.UsuarioResponseDTO response = new com.taller.backend.dto.UsuarioResponseDTO(
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

        return ResponseEntity.ok(response);
    }

    // ---> ACTUALIZADO: Endpoint para guardar cambios (Datos básicos + Password opcional)
    @PostMapping("/actualizar-perfil")
    public ResponseEntity<?> actualizarPerfil(@RequestBody ActualizarPerfilRequest request) {

        // 1. Descubrimos quién es el usuario que hizo clic
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Actualizamos datos básicos (siempre)
        usuario.setNombre(request.getNombre());
        usuario.setApellido(request.getApellido());

        // 3. Lógica de Cambio de Contraseña (Solo si viene una passwordNueva)
        if (request.getPasswordNueva() != null && !request.getPasswordNueva().trim().isEmpty()) {

            // Verificamos la password actual por seguridad
            if (!passwordEncoder.matches(request.getPasswordActual(), usuario.getPassword())) {
                return ResponseEntity.badRequest().body("La contraseña actual es incorrecta. No se pudo actualizar la clave.");
            }

            // Encriptamos y guardamos la nueva
            usuario.setPassword(passwordEncoder.encode(request.getPasswordNueva()));
        }

        // 4. Guardamos todo en la base de datos
        usuarioRepository.save(usuario);

        // Devolvemos un JSON prolijo
        Map<String, String> response = new HashMap<>();
        response.put("mensaje", "¡Perfil actualizado con éxito!");

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // ENDPOINTS PARA GESTIÓN DE EQUIPO (MECÁNICOS)
    // =========================================================================

    @GetMapping("/equipo")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<java.util.List<com.taller.backend.dto.UsuarioResponseDTO>> listarEquipo() {
        Taller taller = securityHelper.getTallerAutenticado();
        java.util.List<Usuario> equipo = usuarioRepository.findByTallerIdAndRol(taller.getId(), RolUsuario.MECANICO);
        
        java.util.List<com.taller.backend.dto.UsuarioResponseDTO> response = equipo.stream()
            .map(u -> new com.taller.backend.dto.UsuarioResponseDTO(
                u.getId(), u.getEmail(), u.getNombre(), u.getApellido(), u.getRol(),
                taller.getId(), taller.getNombre(), taller.getEstadoSuscripcion(), taller.getTipoPlan(), taller.getFechaVencimiento()
            )).collect(java.util.stream.Collectors.toList());
            
        return ResponseEntity.ok(response);
    }

    @PostMapping("/equipo")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> crearMiembroEquipo(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String nombre = request.get("nombre");
        String apellido = request.get("apellido");
        String password = request.get("password");

        if (usuarioRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("El correo ya está registrado.");
        }

        Taller taller = securityHelper.getTallerAutenticado();

        Usuario mecanico = new Usuario();
        mecanico.setEmail(email);
        mecanico.setNombre(nombre);
        mecanico.setApellido(apellido);
        mecanico.setPassword(passwordEncoder.encode(password));
        mecanico.setRol(RolUsuario.MECANICO);
        mecanico.setTaller(taller);

        usuarioRepository.save(mecanico);

        return ResponseEntity.ok(new com.taller.backend.dto.UsuarioResponseDTO(
            mecanico.getId(), mecanico.getEmail(), mecanico.getNombre(), mecanico.getApellido(), mecanico.getRol(),
            taller.getId(), taller.getNombre(), taller.getEstadoSuscripcion(), taller.getTipoPlan(), taller.getFechaVencimiento()
        ));
    }

    @DeleteMapping("/equipo/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> eliminarMiembroEquipo(@PathVariable Long id) {
        Taller taller = securityHelper.getTallerAutenticado();
        
        return usuarioRepository.findById(id).map(usuario -> {
            if (!usuario.getTaller().getId().equals(taller.getId()) || !usuario.getRol().equals(RolUsuario.MECANICO)) {
                throw new RuntimeException("Acceso denegado o usuario inválido");
            }
            usuarioRepository.delete(usuario);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}