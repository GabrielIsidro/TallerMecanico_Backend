package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.dto.NuevaOrdenRequest;
import com.taller.backend.modules.talleres.model.ItemOrden;
import com.taller.backend.modules.talleres.model.OrdenTrabajo;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import com.taller.backend.modules.talleres.service.OrdenTrabajoService;
import com.taller.backend.modules.talleres.model.EstadoOrden;
import com.taller.backend.modules.talleres.model.FormaPago;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.core.service.PdfService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController 
@RequestMapping("/api/v1/talleres/ordenes") 
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.DELETE})
public class OrdenTrabajoController {

    @Autowired
    private OrdenTrabajoService ordenService;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Autowired
    private PdfService pdfService;

    @PostMapping
    public ResponseEntity<OrdenTrabajo> crearOrden(@RequestBody NuevaOrdenRequest request) {
        try {
            OrdenTrabajo nuevaOrden = ordenService.crearOrden(request);
            return ResponseEntity.ok(nuevaOrden);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenTrabajo> actualizarOrden(@PathVariable Long id, @RequestBody NuevaOrdenRequest request) {
        try {
            OrdenTrabajo ordenActualizada = ordenService.actualizarOrdenCompleta(id, request);
            return ResponseEntity.ok(ordenActualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<OrdenTrabajo>> listarOrdenes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<OrdenTrabajo> ordenesPage = ordenService.obtenerTodas(pageable);
        
        return ResponseEntity.ok(ordenesPage);
    }

    @GetMapping("/vehiculo/{id}")
    public List<OrdenTrabajo> listarPorVehiculo(@PathVariable Long id) {
        return ordenService.obtenerPorVehiculo(id);
    }

    @PatchMapping("/{id}/estado")
    public OrdenTrabajo cambiarEstado(@PathVariable Long id, @RequestParam EstadoOrden estado){
        return ordenService.actualizarEstado(id, estado);
    }

    @PatchMapping("/{id}/costo")
    public OrdenTrabajo cambiarCosto(@PathVariable Long id, @RequestParam Double costo){
        return ordenService.actualizarCosto(id, costo);
    }

    @PatchMapping("/{id}/pago")
    public OrdenTrabajo cambiarPago(@PathVariable Long id, @RequestParam FormaPago formaPago){
        return ordenService.actualizarPago(id, formaPago);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        OrdenTrabajo orden = ordenTrabajoRepository.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        
        byte[] pdfBytes = pdfService.generarOrdenPdf(orden);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Orden_" + id + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}
