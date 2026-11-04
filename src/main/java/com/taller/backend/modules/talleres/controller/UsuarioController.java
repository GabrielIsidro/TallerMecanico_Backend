package com.taller.backend.modules.talleres.controller;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.DuplicateResourceException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.dto.ActualizarPerfilRequest;
import com.taller.backend.modules.talleres.dto.MecanicoProductividadDTO;
import com.taller.backend.modules.talleres.dto.UsuarioResponseDTO;
import com.taller.backend.modules.talleres.model.EstadoOrden;
import com.taller.backend.modules.talleres.model.OrdenTrabajo;
import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

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
        response.setCancelacionProgramada(usuario.getTaller() != null && Boolean.TRUE.equals(usuario.getTaller().getCancelacionProgramada()));

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
    @PreAuthorize("hasAnyRole('ADMIN_TALLER', 'MECANICO')")
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

    @GetMapping("/equipo/productividad")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MecanicoProductividadDTO>> obtenerProductividadEquipo() {
        Taller taller = securityHelper.getTallerAutenticado();
        List<Usuario> equipo = usuarioRepository.findByTallerIdAndRol(taller.getId(), RolUsuario.MECANICO);
        List<OrdenTrabajo> todasLasOrdenes = ordenTrabajoRepository.findByTaller(taller);

        LocalDate hoy = LocalDate.now();

        List<MecanicoProductividadDTO> resultado = new ArrayList<>();

        for (Usuario m : equipo) {
            List<OrdenTrabajo> ordenesMecanico = todasLasOrdenes.stream()
                    .filter(o -> o.getMecanicoAsignado() != null && o.getMecanicoAsignado().getId().equals(m.getId()))
                    .collect(Collectors.toList());

            List<MecanicoProductividadDTO.OrdenActivaMecanicoDTO> ordenesActivas = new ArrayList<>();
            int enCurso = 0;
            int finalizadosHoy = 0;
            int entregadosHoy = 0;
            int totalHistorico = 0;

            for (OrdenTrabajo o : ordenesMecanico) {
                boolean esActiva = o.getEstado() == EstadoOrden.PENDIENTE
                        || o.getEstado() == EstadoOrden.PRESUPUESTADO
                        || o.getEstado() == EstadoOrden.APROBADO
                        || o.getEstado() == EstadoOrden.EN_REPARACION;

                if (esActiva) {
                    enCurso++;
                    ordenesActivas.add(MecanicoProductividadDTO.OrdenActivaMecanicoDTO.builder()
                            .ordenId(o.getId())
                            .patente(o.getVehiculo() != null ? o.getVehiculo().getPatente() : "-")
                            .marca(o.getVehiculo() != null ? o.getVehiculo().getMarca() : "")
                            .modelo(o.getVehiculo() != null ? o.getVehiculo().getModelo() : "")
                            .clienteNombre(o.getVehiculo() != null && o.getVehiculo().getCliente() != null ? o.getVehiculo().getCliente().getNombreCliente() : "Sin dueño")
                            .estado(o.getEstado().name())
                            .descripcion(o.getDescripcion())
                            .build());
                }

                LocalDate fechaAccion = o.getFechaActualizacion() != null 
                        ? o.getFechaActualizacion().toLocalDate() 
                        : (o.getFechaIngreso() != null ? o.getFechaIngreso().toLocalDate() : null);

                if (o.getEstado() == EstadoOrden.FINALIZADO || o.getEstado() == EstadoOrden.ENTREGADO) {
                    totalHistorico++;
                    if (hoy.equals(fechaAccion)) {
                        if (o.getEstado() == EstadoOrden.FINALIZADO) {
                            finalizadosHoy++;
                        } else if (o.getEstado() == EstadoOrden.ENTREGADO) {
                            entregadosHoy++;
                        }
                    }
                }
            }

            resultado.add(MecanicoProductividadDTO.builder()
                    .id(m.getId())
                    .nombre(m.getNombre())
                    .apellido(m.getApellido())
                    .email(m.getEmail())
                    .ordenesEnCurso(enCurso)
                    .finalizadosHoy(finalizadosHoy)
                    .entregadosHoy(entregadosHoy)
                    .totalHistoricoFinalizados(totalHistorico)
                    .ordenesActivas(ordenesActivas)
                    .build());
        }

        return ResponseEntity.ok(resultado);
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

        if (taller.getTipoPlan() != com.taller.backend.modules.backoffice.model.TipoPlan.PRO) {
            throw new BusinessRuleException("La gestión de mecánicos y equipo de trabajo es una función exclusiva del Plan PRO. Mejorá tu suscripción para sumar colaboradores.");
        }

        Usuario mecanico = new Usuario();
        mecanico.setEmail(email.trim());
        mecanico.setNombre(nombre);
        mecanico.setApellido(apellido);
        mecanico.setPassword(passwordEncoder.encode(password.trim()));
        mecanico.setRol(RolUsuario.MECANICO);
        mecanico.setTaller(taller);
        mecanico.setTallerId(taller.getId());

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
