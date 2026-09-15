package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.TipoServicio;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import com.taller.backend.modules.talleres.service.DataImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/talleres/servicios")
@CrossOrigin(origins = "*")
public class TipoServicioController {

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private DataImportService dataImportService;

    @PostMapping("/importar")
    public ResponseEntity<String> importarPrecios(@RequestParam("file") MultipartFile file) {
        try {
            String resultado = dataImportService.importarPrecios(file); 
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error al importar: " + e.getMessage());
        }
    }
    
    @GetMapping
    public List<TipoServicio> listarServicios() {
        return tipoServicioRepository.findAll();
    }

    @PostMapping
    public TipoServicio guardar(@RequestBody TipoServicio tipoServicio){
        return tipoServicioRepository.save(tipoServicio);
    }

    @PutMapping("/{id}")
    public TipoServicio actualizar(@PathVariable Long id, @RequestBody TipoServicio detalles){
        TipoServicio servicio = tipoServicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado con ID: " + id));
        
        servicio.setGrupo(detalles.getGrupo());
        servicio.setDescripcion(detalles.getDescripcion());
        servicio.setPrecioSugerido(detalles.getPrecioSugerido()); 

        return tipoServicioRepository.save(servicio);
    }

    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id){
        tipoServicioRepository.deleteById(id);
    }

}
