package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.model.Repuesto;
import com.taller.backend.modules.talleres.service.RepuestoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/talleres/repuestos")
@CrossOrigin(origins = "*")
public class RepuestoController {

    @Autowired
    private RepuestoService repuestoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_TALLER', 'MECANICO')")
    public ResponseEntity<?> listarRepuestos(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Boolean bajoStock) {
        if (page != null) {
            int pageSize = size != null ? size : 10;
            Pageable pageable = PageRequest.of(page, pageSize, Sort.by("id").descending());
            return ResponseEntity.ok(repuestoService.obtenerRepuestosPaginados(pageable, search, bajoStock));
        }
        return ResponseEntity.ok(repuestoService.listarRepuestos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_TALLER', 'MECANICO')")
    public ResponseEntity<Repuesto> obtenerRepuestoPorId(@PathVariable Long id) {
        return ResponseEntity.ok(repuestoService.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<Repuesto> crearRepuesto(@Valid @RequestBody Repuesto repuesto) {
        Repuesto nuevo = repuestoService.guardarRepuesto(repuesto);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<Repuesto> actualizarRepuesto(@PathVariable Long id, @Valid @RequestBody Repuesto detalles) {
        Repuesto actualizado = repuestoService.actualizarRepuesto(id, detalles);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_TALLER')")
    public ResponseEntity<?> eliminarRepuesto(@PathVariable Long id) {
        repuestoService.eliminarRepuesto(id);
        return ResponseEntity.ok().build();
    }
}
