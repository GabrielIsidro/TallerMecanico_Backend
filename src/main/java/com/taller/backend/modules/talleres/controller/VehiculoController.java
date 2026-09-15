package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.model.Vehiculo;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/talleres/vehiculos")
@CrossOrigin(origins = "*")
public class VehiculoController {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @GetMapping
    public List<Vehiculo> obtenerVehiculos() {
        return vehiculoRepository.findAll();
    }

    @PostMapping
    public Vehiculo crearVehiculo(@RequestBody Vehiculo vehiculo) {
        return vehiculoRepository.save(vehiculo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizarVehiculo(@PathVariable Long id, @RequestBody Vehiculo detallesVehiculo) {
        return vehiculoRepository.findById(id).map(vehiculo -> {
            vehiculo.setPatente(detallesVehiculo.getPatente());
            vehiculo.setModelo(detallesVehiculo.getModelo());
            vehiculo.setMarca(detallesVehiculo.getMarca());
            vehiculo.setAnio(detallesVehiculo.getAnio());
            vehiculo.setNumeroMotor(detallesVehiculo.getNumeroMotor());
            vehiculo.setNumeroChasis(detallesVehiculo.getNumeroChasis());
            vehiculo.setKilometraje(detallesVehiculo.getKilometraje());
            vehiculo.setProximoServiceKm(detallesVehiculo.getProximoServiceKm());
            vehiculo.setCliente(detallesVehiculo.getCliente());

            return ResponseEntity.ok(vehiculoRepository.save(vehiculo));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVehiculo(@PathVariable Long id) {
        return vehiculoRepository.findById(id).map(vehiculo -> {
            vehiculoRepository.delete(vehiculo);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
