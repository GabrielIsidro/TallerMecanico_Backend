package com.taller.backend.controller;

import com.taller.backend.model.PlanSuscripcion;
import com.taller.backend.repository.PlanSuscripcionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planes")
@CrossOrigin(origins = "*")
public class PlanSuscripcionController {

    @Autowired
    private PlanSuscripcionRepository planRepository;

    // Obtener todos los planes (Público / Autenticado)
    @GetMapping
    public ResponseEntity<List<PlanSuscripcion>> listarPlanes() {
        return ResponseEntity.ok(planRepository.findAll());
    }

    // Editar precio y estado de un plan (Solo SuperAdmin)
    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> editarPlan(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return planRepository.findById(id).map(plan -> {
            if (request.containsKey("precio")) {
                plan.setPrecio(Double.valueOf(request.get("precio").toString()));
            }
            if (request.containsKey("activo")) {
                plan.setActivo(Boolean.valueOf(request.get("activo").toString()));
            }
            if (request.containsKey("descripcion")) {
                plan.setDescripcion(request.get("descripcion").toString());
            }
            planRepository.save(plan);
            return ResponseEntity.ok(plan);
        }).orElse(ResponseEntity.notFound().build());
    }
}
