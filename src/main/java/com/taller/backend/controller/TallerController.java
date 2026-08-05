package com.taller.backend.controller;

import com.taller.backend.model.RolUsuario;
import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.dto.TallerRegistroDTO;
import com.taller.backend.repository.TallerRepository;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.service.EmailService; // <--- Importamos el servicio de correo
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/talleres")
@CrossOrigin(origins = "*")
public class TallerController {

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---> 1. Inyectamos a nuestro Cartero
    @Autowired
    private EmailService emailService;

    @GetMapping
    public List<Taller> obtenerTalleres() {
        return tallerRepository.findAll(); 
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crearTaller(@RequestBody TallerRegistroDTO dto) {
        
        if (dto.getEmailContacto() == null || dto.getEmailContacto().isEmpty()) {
            return ResponseEntity.badRequest().body("El email es obligatorio para crear el usuario del sistema.");
        }

        if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body("La contraseña es obligatoria.");
        }

        if (usuarioRepository.findByEmail(dto.getEmailContacto()).isPresent()) {
            return ResponseEntity.badRequest().body("El email ya está registrado.");
        }

        // Crear la entidad Taller a partir del DTO
        Taller taller = new Taller();
        taller.setNombre(dto.getNombre());
        taller.setTitular(dto.getTitular());
        taller.setTelefono(dto.getTelefono());
        taller.setEmailContacto(dto.getEmailContacto());
        // El estado y fecha de vencimiento se inicializan por defecto

        Taller tallerGuardado = tallerRepository.save(taller);

        // Usamos la password proporcionada por el SuperAdmin
        String passwordFinal = dto.getPassword();

        Usuario adminTaller = new Usuario();
        adminTaller.setEmail(dto.getEmailContacto()); 
        
        // Si nos pasan nombreAdmin, lo usamos, si no usamos el Titular
        adminTaller.setNombre(dto.getNombreAdmin() != null && !dto.getNombreAdmin().isEmpty() ? dto.getNombreAdmin() : dto.getTitular());
        adminTaller.setApellido(dto.getApellidoAdmin() != null ? dto.getApellidoAdmin() : ""); 
        
        // Guardamos la clave encriptada en la base de datos
        adminTaller.setPassword(passwordEncoder.encode(passwordFinal)); 
        adminTaller.setRol(RolUsuario.ADMIN_TALLER); 
        adminTaller.setTaller(tallerGuardado); 

        usuarioRepository.save(adminTaller);

        // ---> 3. Hacemos que el cartero mande el mail con la clave manual
        try {
            emailService.enviarEmailBienvenida(taller.getEmailContacto(), taller.getNombre(), passwordFinal);
        } catch (Exception e) {
            // Si falla el envío de correo (ej: sin internet), lo registramos pero no borramos el taller
            System.err.println("Error al enviar el correo de bienvenida: " + e.getMessage());
        }

        return ResponseEntity.ok(tallerGuardado);
    }
    
    @PatchMapping("/{id}/suscripcion")
    public ResponseEntity<?> cambiarSuscripcion(@PathVariable Long id, @RequestParam String estado) {
        return tallerRepository.findById(id).map(taller -> {
            taller.setEstadoSuscripcion(com.taller.backend.model.EstadoSuscripcion.valueOf(estado.toUpperCase()));
            tallerRepository.save(taller);
            return ResponseEntity.ok(taller);
        }).orElse(ResponseEntity.notFound().build());
    }
}