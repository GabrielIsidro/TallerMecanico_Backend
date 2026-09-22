package com.taller.backend.modules.talleres.controller;

import com.taller.backend.core.service.PdfService;
import com.taller.backend.modules.talleres.dto.ActualizarOrdenRequest;
import com.taller.backend.modules.talleres.dto.NuevaOrdenRequest;
import com.taller.backend.modules.talleres.model.EstadoOrden;
import com.taller.backend.modules.talleres.model.FormaPago;
import com.taller.backend.modules.talleres.model.OrdenTrabajo;
import com.taller.backend.modules.talleres.service.OrdenTrabajoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/talleres/ordenes") 
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.DELETE})
public class OrdenTrabajoController {

    @Autowired
    private OrdenTrabajoService ordenService;

    @Autowired
    private PdfService pdfService;

    @PostMapping
    public ResponseEntity<OrdenTrabajo> crearOrden(@Valid @RequestBody NuevaOrdenRequest request) {
        OrdenTrabajo nuevaOrden = ordenService.crearOrden(request);
        return ResponseEntity.ok(nuevaOrden);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrdenTrabajo> actualizarOrden(@PathVariable Long id, @Valid @RequestBody ActualizarOrdenRequest request) {
        OrdenTrabajo ordenActualizada = ordenService.actualizarOrdenCompleta(id, request);
        return ResponseEntity.ok(ordenActualizada);
    }

    @GetMapping
    public ResponseEntity<Page<OrdenTrabajo>> listarOrdenes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<OrdenTrabajo> ordenesPage = ordenService.obtenerTodas(pageable);
        
        return ResponseEntity.ok(ordenesPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenTrabajo> obtenerPorId(@PathVariable Long id) {
        OrdenTrabajo orden = ordenService.obtenerPorId(id);
        return ResponseEntity.ok(orden);
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
        OrdenTrabajo orden = ordenService.obtenerPorId(id);
        
        byte[] pdfBytes = pdfService.generarOrdenPdf(orden);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Orden_" + id + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}
