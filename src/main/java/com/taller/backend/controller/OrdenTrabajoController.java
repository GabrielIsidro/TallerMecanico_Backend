package com.taller.backend.controller;

import com.taller.backend.model.ItemOrden;
import com.taller.backend.model.OrdenTrabajo;
// import com.taller.backend.model.OrdenTrabajo;
import com.taller.backend.service.OrdenTrabajoService;
import com.taller.backend.model.EstadoOrden;
// import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.ResponseEntity;

/* 
Controlador para gestionar las operaciones relacionadas con las órdenes de trabajo
 */

@RestController 
@RequestMapping("/api/ordenes") //La ruta base para este controlador
@CrossOrigin(origins = "*") //Permite solicitudes desde cualquier origen (útil para desarrollo)
public class OrdenTrabajoController {

    //Inyección de dependencia para el servicio de ordenes de trabajo
    @Autowired
    private OrdenTrabajoService ordenService;

    /*
    Endpoint para crear una nueva orden de trabajo. Recibe un JSON con los datos necesarios para crear la orden, incluyendo el ID del vehículo, la descripción del problema y una lista de items (servicios) a realizar. Devuelve la orden creada o un error si algo sale mal.
    */
    @PostMapping
    public ResponseEntity<OrdenTrabajo> crearOrden(@RequestBody NuevaOrdenRequest request) {
        try {
            OrdenTrabajo nuevaOrden = ordenService.crearOrden(
                request.getVehiculoId(), 
                request.getDescripcion(), 
                request.getItems()
            );
            return ResponseEntity.ok(nuevaOrden);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /*
    Endpoint para listar todas las órdenes de trabajo
     */
    @GetMapping
    public List<OrdenTrabajo> listarOrdenes() {
        return ordenService.obtenerTodas();
    }

    /*
    Endpoint para ver historial de un auto especifico
     */
    @GetMapping("/vehiculo/{id}")
    public List<OrdenTrabajo> listarPorVehiculo(@PathVariable Long id) {
        return ordenService.obtenerPorVehiculo(id);
    }

    /*
    Endpoint para cambiar el estado de una orden de trabajo
     */
    @PatchMapping("/{id}/estado")
    public OrdenTrabajo cambiarEstado(@PathVariable Long id, @RequestParam EstadoOrden estado){
        return ordenService.actualizarEstado(id, estado);
    }

    /*
    Endpoint para cambiar el costo de una orden de trabajo
     */
    @PatchMapping("/{id}/costo")
    public OrdenTrabajo cambiarCosto(@PathVariable Long id, @RequestParam Double costo){
        return ordenService.actualizarCosto(id, costo);
    }

    /*
    Clase auxiliar (DTO) para recibir los datos limpios desde el JSON
     */
    @lombok.Data
    public static class NuevaOrdenRequest {
        private Long vehiculoId;
        private String descripcion;
        private List<ItemOrden> items;
    }
}
