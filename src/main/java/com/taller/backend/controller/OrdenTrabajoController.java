package com.taller.backend.controller;

import com.taller.backend.dto.NuevaOrdenRequest;
import com.taller.backend.model.ItemOrden;
import com.taller.backend.model.OrdenTrabajo;
import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.UsuarioRepository;
import com.taller.backend.repository.OrdenTrabajoRepository;
import com.taller.backend.repository.VehiculoRepository;
import com.taller.backend.service.OrdenTrabajoService;
import com.taller.backend.model.EstadoOrden;
import com.taller.backend.model.FormaPago;
import com.taller.backend.security.SecurityHelper;
import com.taller.backend.service.PdfService;

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
@RequestMapping("/api/ordenes") 
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PATCH, RequestMethod.PUT, RequestMethod.DELETE})
public class OrdenTrabajoController {

    @Autowired
    private OrdenTrabajoService ordenService;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    // ---> 1. Traemos el helper de seguridad
    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private PdfService pdfService;

    @PostMapping
    public ResponseEntity<OrdenTrabajo> crearOrden(@RequestBody NuevaOrdenRequest request) {
        try {
            // ---> 3. Le pasamos el Taller al servicio para que la orden nazca con la marca de agua
            OrdenTrabajo nuevaOrden = ordenService.crearOrden(
                request.getVehiculoId(), 
                request.getDescripcion(), 
                request.getItems(),
                securityHelper.getTallerAutenticado() // <--- NUEVO PARÁMETRO
            );
            return ResponseEntity.ok(nuevaOrden);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<Page<OrdenTrabajo>> listarOrdenes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<OrdenTrabajo> ordenesPage = ordenService.obtenerPorTaller(securityHelper.getTallerAutenticado(), pageable);
        
        return ResponseEntity.ok(ordenesPage);
    }

    @GetMapping("/vehiculo/{id}")
    public List<OrdenTrabajo> listarPorVehiculo(@PathVariable Long id) {
        return ordenService.obtenerPorVehiculo(id, securityHelper.getTallerAutenticado());
    }

    @PatchMapping("/{id}/estado")
    public OrdenTrabajo cambiarEstado(@PathVariable Long id, @RequestParam EstadoOrden estado){
        return ordenService.actualizarEstado(id, estado, securityHelper.getTallerAutenticado());
    }

    @PatchMapping("/{id}/costo")
    public OrdenTrabajo cambiarCosto(@PathVariable Long id, @RequestParam Double costo){
        return ordenService.actualizarCosto(id, costo, securityHelper.getTallerAutenticado());
    }

    @PatchMapping("/{id}/pago")
    public OrdenTrabajo cambiarPago(@PathVariable Long id, @RequestParam FormaPago formaPago){
        return ordenService.actualizarPago(id, formaPago, securityHelper.getTallerAutenticado());
    }

    // ==========================================
    // NUEVO ENDPOINT: GENERAR PDF DE LA ORDEN
    // ==========================================
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        Taller taller = securityHelper.getTallerAutenticado();
        
        OrdenTrabajo orden = ordenTrabajoRepository.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        
        if (!orden.getVehiculo().getCliente().getTaller().getId().equals(taller.getId())) {
            throw new RuntimeException("No tienes permiso para ver esta orden.");
        }

        byte[] pdfBytes = pdfService.generarOrdenPdf(orden);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Orden_" + id + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

}