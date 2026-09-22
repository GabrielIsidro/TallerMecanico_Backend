package com.taller.backend.modules.backoffice.controller;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.modules.backoffice.dto.TallerRegistroDTO;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.SuperAdmin;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.SuperAdminRepository;
import com.taller.backend.modules.backoffice.service.TallerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/backoffice/admin/saas")
@CrossOrigin(origins = "*")
public class SaaSAdminController {

    @Autowired
    private TallerService tallerService;

    @Autowired
    private SuperAdminRepository superAdminRepository;

    // Endpoint para que el Frontend obtenga el rol y perfil del admin
    @GetMapping("/me")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerMiPerfil() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        SuperAdmin superAdmin = superAdminRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con email: " + email));

        Map<String, Object> response = new HashMap<>();
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
        return tallerService.listarTalleres();
    }

    @PostMapping("/talleres")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> crearTaller(@RequestBody Map<String, String> request) {
        TallerRegistroDTO dto = new TallerRegistroDTO();
        dto.setNombre(request.get("nombreTaller") != null ? request.get("nombreTaller") : request.get("nombre"));
        dto.setTitular(request.get("titular"));
        dto.setTelefono(request.get("telefono"));
        dto.setEmailContacto(request.get("emailAdmin") != null ? request.get("emailAdmin") : request.get("emailContacto"));
        dto.setPassword(request.get("passwordAdmin") != null ? request.get("passwordAdmin") : request.get("password"));
        dto.setNombreAdmin(request.get("nombreAdmin"));
        dto.setApellidoAdmin(request.get("apellidoAdmin"));

        tallerService.crearTaller(dto);
        return ResponseEntity.ok("Taller y administrador creados con éxito.");
    }

    @PutMapping("/talleres/{id}/suscripcion")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> actualizarSuscripcion(
            @PathVariable Long id,
            @RequestParam EstadoSuscripcion estado,
            @RequestParam(required = false) Integer diasExtension) {
        tallerService.actualizarSuscripcion(id, estado, diasExtension);
        return ResponseEntity.ok("Suscripción del taller actualizada.");
    }

    @DeleteMapping("/talleres/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> eliminarTaller(@PathVariable Long id) {
        tallerService.eliminarTaller(id);
        return ResponseEntity.ok("Taller y usuarios vinculados eliminados correctamente.");
    }

    @DeleteMapping("/usuarios/{email}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> eliminarUsuarioPorEmail(@PathVariable String email) {
        tallerService.eliminarUsuarioPorEmail(email);
        return ResponseEntity.ok("Usuario " + email + " eliminado correctamente. El correo ya está libre.");
    }
}
