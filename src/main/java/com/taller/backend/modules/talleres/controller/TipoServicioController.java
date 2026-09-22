package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.model.TipoServicio;
import com.taller.backend.modules.talleres.service.DataImportService;
import com.taller.backend.modules.talleres.service.TipoServicioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/talleres/servicios")
@CrossOrigin(origins = "*")
public class TipoServicioController {

    @Autowired
    private TipoServicioService tipoServicioService;

    @Autowired
    private DataImportService dataImportService;

    @PostMapping("/importar")
    public ResponseEntity<String> importarPrecios(@RequestParam("file") MultipartFile file) {
        try {
            String resultado = dataImportService.importarPrecios(file); 
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al importar: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ResponseEntity<List<TipoServicio>> listarServicios(@RequestParam(required = false) String grupo) {
        return ResponseEntity.ok(tipoServicioService.listarServicios(grupo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TipoServicio> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(tipoServicioService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<TipoServicio> guardar(@Valid @RequestBody TipoServicio tipoServicio) {
        TipoServicio nuevo = tipoServicioService.guardar(tipoServicio);
        return new ResponseEntity<>(nuevo, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TipoServicio> actualizar(@PathVariable Long id, @Valid @RequestBody TipoServicio detalles) {
        TipoServicio actualizado = tipoServicioService.actualizar(id, detalles);
        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        tipoServicioService.eliminar(id);
        return ResponseEntity.ok().build();
    }
}
