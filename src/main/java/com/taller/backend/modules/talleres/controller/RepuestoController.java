package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.model.Repuesto;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.repository.RepuestoRepository;
import com.taller.backend.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/talleres/repuestos")
@CrossOrigin(origins = "*")
public class RepuestoController {

    @Autowired
    private RepuestoRepository repuestoRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_TALLER', 'MECANICO')")
    public ResponseEntity<List<Repuesto>> listarRepuestos() {
        return ResponseEntity.ok(repuestoRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<Repuesto> crearRepuesto(@RequestBody Repuesto repuesto) {
        return ResponseEntity.ok(repuestoRepository.save(repuesto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<Repuesto> actualizarRepuesto(@PathVariable Long id, @RequestBody Repuesto detalles) {
        return repuestoRepository.findById(id).map(repuesto -> {
            repuesto.setNombre(detalles.getNombre());
            repuesto.setSku(detalles.getSku());
            repuesto.setCantidad(detalles.getCantidad());
            repuesto.setStockMinimo(detalles.getStockMinimo());
            repuesto.setPrecio(detalles.getPrecio());
            return ResponseEntity.ok(repuestoRepository.save(repuesto));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> eliminarRepuesto(@PathVariable Long id) {
        return repuestoRepository.findById(id).map(repuesto -> {
            repuestoRepository.delete(repuesto);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
