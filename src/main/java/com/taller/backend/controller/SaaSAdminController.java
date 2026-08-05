package com.taller.backend.controller;

import com.taller.backend.model.EstadoSuscripcion;
import com.taller.backend.model.RolUsuario;
import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.TallerRepository;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.repository.ClienteRepository;
import com.taller.backend.repository.VehiculoRepository;
import com.taller.backend.repository.OrdenTrabajoRepository;
import com.taller.backend.repository.TipoServicioRepository;
import com.taller.backend.repository.RepuestoRepository;
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
@RequestMapping("/api/admin/saas")
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
    private PasswordEncoder passwordEncoder;

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
        String emailAdmin = request.get("emailAdmin");
        String passwordAdmin = request.get("passwordAdmin");
        
        if (usuarioRepository.findByEmail(emailAdmin).isPresent()) {
            return ResponseEntity.badRequest().body("El email ya está en uso.");
        }

        // 1. Crear el Taller
        Taller nuevoTaller = new Taller();
        nuevoTaller.setNombre(nombreTaller);
        nuevoTaller.setEstadoSuscripcion(EstadoSuscripcion.PRUEBA_GRATUITA);
        nuevoTaller.setFechaVencimiento(LocalDate.now().plusDays(30)); // 30 días de prueba
        tallerRepository.save(nuevoTaller);

        // 2. Crear el Usuario Administrador del Taller
        Usuario adminTaller = new Usuario();
        adminTaller.setEmail(emailAdmin);
        adminTaller.setPassword(passwordEncoder.encode(passwordAdmin));
        adminTaller.setNombre("Admin");
        adminTaller.setApellido(nombreTaller);
        adminTaller.setRol(RolUsuario.ADMIN_TALLER);
        adminTaller.setTaller(nuevoTaller); // Relacionamos el usuario con el taller creado
        usuarioRepository.save(adminTaller);

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
