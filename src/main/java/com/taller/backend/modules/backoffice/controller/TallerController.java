package com.taller.backend.modules.backoffice.controller;

import com.taller.backend.modules.backoffice.dto.TallerRegistroDTO;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.service.TallerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/backoffice/talleres")
@CrossOrigin(origins = "*")
public class TallerController {

    @Autowired
    private TallerService tallerService;

    @GetMapping
    public List<Taller> obtenerTalleres() {
        return tallerService.listarTalleres();
    }

    @PostMapping
    public ResponseEntity<?> crearTaller(@Valid @RequestBody TallerRegistroDTO dto) {
        Taller tallerGuardado = tallerService.crearTaller(dto);
        return ResponseEntity.ok(tallerGuardado);
    }

    @PatchMapping("/{id}/suscripcion")
    public ResponseEntity<?> cambiarSuscripcion(@PathVariable Long id, @RequestParam String estado) {
        Taller taller = tallerService.actualizarSuscripcion(id, EstadoSuscripcion.valueOf(estado.toUpperCase()), null);
        return ResponseEntity.ok(taller);
    }
}
