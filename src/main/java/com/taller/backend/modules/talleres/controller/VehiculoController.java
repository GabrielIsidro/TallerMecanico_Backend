package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.model.Vehiculo;
import com.taller.backend.modules.talleres.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/talleres/vehiculos")
@CrossOrigin(origins = "*")
public class VehiculoController {

    @Autowired
    private VehiculoService vehiculoService;

    @GetMapping
    public ResponseEntity<?> obtenerVehiculos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "") String search) {
        if (page != null) {
            int pageSize = size != null ? size : 10;
            Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());
            return ResponseEntity.ok(vehiculoService.obtenerVehiculosPaginados(pageable, search));
        }
        return ResponseEntity.ok(vehiculoService.getAllVehiculos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> obtenerVehiculoPorId(@PathVariable Long id) {
        Vehiculo vehiculo = vehiculoService.getVehiculoById(id);
        return ResponseEntity.ok(vehiculo);
    }

    @PostMapping
    public ResponseEntity<Vehiculo> crearVehiculo(@Valid @RequestBody Vehiculo vehiculo) {
        Vehiculo nuevoVehiculo = vehiculoService.guardarVehiculo(vehiculo);
        return new ResponseEntity<>(nuevoVehiculo, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> actualizarVehiculo(@PathVariable Long id, @Valid @RequestBody Vehiculo detallesVehiculo) {
        Vehiculo actualizado = vehiculoService.actualizarVehiculo(id, detallesVehiculo);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVehiculo(@PathVariable Long id) {
        vehiculoService.deleteVehiculo(id);
        return ResponseEntity.ok().build();
    }
}
