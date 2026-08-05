package com.taller.backend.controller;

import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.model.Vehiculo;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@CrossOrigin(origins = "*")
public class VehiculoController {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    // ---> 1. Traemos la base de usuarios
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ---> 2. Función para descubrir de qué taller es la persona que hizo clic
    private Taller getTallerAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuario.getTaller();
    }

    @GetMapping
    public List<Vehiculo> obtenerVehiculos() {
        // ---> 3. Traemos solo la flota de SU taller
        Taller miTaller = getTallerAutenticado();
        return vehiculoRepository.findByTaller(miTaller);
    }

    @PostMapping
    public Vehiculo crearVehiculo(@RequestBody Vehiculo vehiculo) {
        // ---> 4. Le estampamos la marca de agua de SU taller al auto nuevo
        vehiculo.setTaller(getTallerAutenticado());
        return vehiculoRepository.save(vehiculo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizarVehiculo(@PathVariable Long id, @RequestBody Vehiculo detallesVehiculo) {
        return vehiculoRepository.findById(id).map(vehiculo -> {
            if (!vehiculo.getTaller().getId().equals(getTallerAutenticado().getId())) {
                throw new RuntimeException("Acceso denegado: El recurso no pertenece a tu taller.");
            }
            vehiculo.setPatente(detallesVehiculo.getPatente());
            vehiculo.setModelo(detallesVehiculo.getModelo());
            vehiculo.setMarca(detallesVehiculo.getMarca());
            vehiculo.setAnio(detallesVehiculo.getAnio());
            vehiculo.setCategoria(detallesVehiculo.getCategoria());
            vehiculo.setNumeroMotor(detallesVehiculo.getNumeroMotor());
            vehiculo.setNumeroChasis(detallesVehiculo.getNumeroChasis());
            vehiculo.setKilometraje(detallesVehiculo.getKilometraje());
            vehiculo.setProximoServiceKm(detallesVehiculo.getProximoServiceKm());
            vehiculo.setCliente(detallesVehiculo.getCliente());
            // No tocamos el Taller, mantenemos el original
            
            return ResponseEntity.ok(vehiculoRepository.save(vehiculo));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVehiculo(@PathVariable Long id) {
        return vehiculoRepository.findById(id).map(vehiculo -> {
            if (!vehiculo.getTaller().getId().equals(getTallerAutenticado().getId())) {
                throw new RuntimeException("Acceso denegado: El recurso no pertenece a tu taller.");
            }
            vehiculoRepository.delete(vehiculo);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}