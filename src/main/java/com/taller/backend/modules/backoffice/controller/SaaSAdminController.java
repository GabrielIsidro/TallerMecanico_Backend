package com.taller.backend.modules.backoffice.controller;

import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import com.taller.backend.modules.talleres.repository.ClienteRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import com.taller.backend.modules.talleres.repository.RepuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backoffice/admin/saas")
@CrossOrigin(origins = "*")
public class SaaSAdminController {

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ClienteRepository clienteRepository;
    
    @Autowired
    private VehiculoRepository vehiculoRepository;
    
    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;
    
    @Autowired
    private TipoServicioRepository tipoServicioRepository;
    
    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private com.taller.backend.modules.backoffice.repository.SuperAdminRepository superAdminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Endpoint para que el Frontend obtenga el rol y perfil del admin
    @GetMapping("/me")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerMiPerfil() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.taller.backend.modules.backoffice.model.SuperAdmin superAdmin = superAdminRepository
                .findByEmail(email).orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", superAdmin.getId());
        response.put("email", superAdmin.getEmail());
        response.put("nombre", superAdmin.getNombre());
        response.put("rol", "SUPERADMIN");

        return ResponseEntity.ok(response);
    }

    // Solo el SUPER_ADMIN puede acceder a estos endpoints
    @GetMapping("/talleres")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public List<Taller> listarTodosLosTalleres() {
        return tallerRepository.findAll();
    }

    @PostMapping("/talleres")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> crearTaller(@RequestBody Map<String, String> request) {
        String nombreTaller = request.get("nombreTaller");
        String titular = request.get("titular");
        String telefono = request.get("telefono");
        String emailAdmin = request.get("emailAdmin");
        String passwordAdmin = request.get("passwordAdmin");
        String nombreAdmin = request.get("nombreAdmin");
        String apellidoAdmin = request.get("apellidoAdmin");
        
        if (usuarioRepository.findByEmail(emailAdmin).isPresent()) {
            return ResponseEntity.badRequest().body("El email ya está en uso.");
        }

        // 1. Crear el Taller
        Taller nuevoTaller = new Taller();
        nuevoTaller.setNombre(nombreTaller);
        nuevoTaller.setTitular(titular);
        nuevoTaller.setTelefono(telefono);
        nuevoTaller.setEmailContacto(emailAdmin);
        nuevoTaller.setEstadoSuscripcion(EstadoSuscripcion.PRUEBA_GRATUITA);
        nuevoTaller.setFechaVencimiento(LocalDate.now().plusDays(30)); // 30 días de prueba
        tallerRepository.save(nuevoTaller);

        // 2. Crear el Usuario Administrador del Taller usando Native Query para evitar el bloqueo del TenantId de Hibernate
        try {
            usuarioRepository.insertAdminTaller(
                emailAdmin,
                passwordEncoder.encode(passwordAdmin),
                nombreAdmin != null && !nombreAdmin.isEmpty() ? nombreAdmin : "Admin",
                apellidoAdmin != null && !apellidoAdmin.isEmpty() ? apellidoAdmin : nombreTaller,
                RolUsuario.ADMIN_TALLER.name(),
                nuevoTaller.getId()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al crear el administrador del taller: " + e.getMessage());
        }

        return ResponseEntity.ok("Taller y administrador creados con éxito.");
    }

    @PutMapping("/talleres/{id}/suscripcion")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> actualizarSuscripcion(@PathVariable Long id, @RequestParam EstadoSuscripcion estado, @RequestParam(required = false) Integer diasExtension) {
        Taller taller = tallerRepository.findById(id).orElseThrow(() -> new RuntimeException("Taller no encontrado"));
        
        taller.setEstadoSuscripcion(estado);
        if (diasExtension != null) {
            taller.setFechaVencimiento(LocalDate.now().plusDays(diasExtension));
        }

        tallerRepository.save(taller);
        return ResponseEntity.ok("Suscripción del taller actualizada.");
    }

    @DeleteMapping("/talleres/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<?> eliminarTaller(@PathVariable Long id) {
        if (!tallerRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Primero borramos todas las entidades vinculadas para evitar constraint FK
        ordenTrabajoRepository.deleteByTallerId(id);
        vehiculoRepository.deleteByTallerId(id);
        clienteRepository.deleteByTallerId(id);
        tipoServicioRepository.deleteByTallerId(id);
        repuestoRepository.deleteByTallerId(id);
        usuarioRepository.deleteByTallerId(id);
        
        // Ahora borramos el taller
        tallerRepository.deleteById(id);
        
        return ResponseEntity.ok("Taller y usuarios vinculados eliminados correctamente.");
    }

    @DeleteMapping("/usuarios/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> eliminarUsuarioPorEmail(@PathVariable String email) {
        return usuarioRepository.findByEmail(email).map(usuario -> {
            usuarioRepository.delete(usuario);
            return ResponseEntity.ok("Usuario " + email + " eliminado correctamente. El correo ya está libre.");
        }).orElse(ResponseEntity.badRequest().body("No se encontró ningún usuario con el correo: " + email));
    }
}
