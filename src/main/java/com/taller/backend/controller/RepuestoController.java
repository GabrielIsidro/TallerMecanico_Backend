package com.taller.backend.controller;

import com.taller.backend.model.Repuesto;
import com.taller.backend.model.Taller;
import com.taller.backend.repository.RepuestoRepository;
import com.taller.backend.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repuestos")
@CrossOrigin(origins = "*")
public class RepuestoController {

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private SecurityHelper securityHelper;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_TALLER', 'MECANICO')")
    public ResponseEntity<List<Repuesto>> listarRepuestos() {
        Taller taller = securityHelper.getTallerAutenticado();
        List<Repuesto> repuestos = repuestoRepository.findByTallerId(taller.getId());
        return ResponseEntity.ok(repuestos);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<Repuesto> crearRepuesto(@RequestBody Repuesto repuesto) {
        Taller taller = securityHelper.getTallerAutenticado();
        repuesto.setTaller(taller);
        Repuesto guardado = repuestoRepository.save(repuesto);
        return ResponseEntity.ok(guardado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<Repuesto> actualizarRepuesto(@PathVariable Long id, @RequestBody Repuesto detalles) {
        Taller taller = securityHelper.getTallerAutenticado();
        
        return repuestoRepository.findById(id).map(repuesto -> {
            if (!repuesto.getTaller().getId().equals(taller.getId())) {
                throw new RuntimeException("Acceso denegado a este recurso");
            }
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
        Taller taller = securityHelper.getTallerAutenticado();
        return repuestoRepository.findById(id).map(repuesto -> {
            if (!repuesto.getTaller().getId().equals(taller.getId())) {
                throw new RuntimeException("Acceso denegado a este recurso");
            }
            repuestoRepository.delete(repuesto);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
