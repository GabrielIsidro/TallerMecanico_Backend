package com.taller.backend.service;

import com.taller.backend.model.*;
import com.taller.backend.repository.TallerRepository;
import com.taller.backend.repository.OrdenTrabajoRepository;
import com.taller.backend.repository.VehiculoRepository;
import com.taller.backend.repository.TipoServicioRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class OrdenTrabajoService {

    @Autowired
    private OrdenTrabajoRepository ordenRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private EmailService emailService;

    // ---> CAMBIO: Recibe el Taller como parámetro desde el Controlador
    @Transactional
    public OrdenTrabajo crearOrden(Long vehiculoId, String descripcion, List<ItemOrden> items, Taller taller) {
        
        // 1. Buscamos el vehiculo
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));

        // 2. Creamos la orden de trabajo
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setTaller(taller); // ---> Usamos el taller dinámico, chau 1L
        orden.setVehiculo(vehiculo);
        orden.setDescripcion(descripcion);
        orden.setEstado(EstadoOrden.PENDIENTE);
        
        // 3. Procesamos cada item de la orden
        for (ItemOrden item : items) {

            // Si el item viene con un ID de servicio
            if(item.getTipoServicio() != null && item.getTipoServicio().getId() != null) {
                // Buscamos el precio real en la BD
                TipoServicio servicioDb = tipoServicioRepository.findById(item.getTipoServicio().getId())
                        .orElseThrow(() -> new RuntimeException ("Servicio no encontrado"));
                
                item.setTipoServicio(servicioDb);

                // Le pasamos la categoria del auto al item para que elija el precio correcto
                item.calcularSubtotal(vehiculo.getCategoria());
            }

            // Agregamos el item a la orden
            orden.agregarItem(item);
        }

        // 4. Guardamos
        return ordenRepository.save(orden);
    }

    // ---> CAMBIO: Renombrado a obtenerPorTaller y recibe el parámetro
    public Page<OrdenTrabajo> obtenerPorTaller(Taller taller, Pageable pageable) {
        return ordenRepository.findByTaller(taller, pageable);
    }

    // ---> CAMBIO: Le pasamos el taller por si es una orden nueva desde otro lado
    public OrdenTrabajo guardarOrden(OrdenTrabajo orden, Taller taller) {
        // Valida que venga con un vehiculo 
        if (orden.getVehiculo() != null && orden.getVehiculo().getId() != null) {
            Vehiculo vehiculoReal = vehiculoRepository.findById(orden.getVehiculo().getId()).orElse(null);
            if (vehiculoReal != null) {
                orden.setVehiculo(vehiculoReal);
            }
        }
        
        // Si no tiene taller asignado, se lo ponemos por seguridad usando el dinámico
        if (orden.getTaller() == null) {
            orden.setTaller(taller);
        }

        return ordenRepository.save(orden);
    }

    public OrdenTrabajo actualizarPago(Long id, FormaPago formaPago, Taller taller) {
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        if (!orden.getTaller().getId().equals(taller.getId())) {
            throw new RuntimeException("Acceso denegado: La orden no pertenece a tu taller.");
        }
        orden.setFormaPago(formaPago);
        return ordenRepository.save(orden);
    }
    
    // Metodo para obtener las ordenes de trabajo por el id del vehiculo
    public List<OrdenTrabajo> obtenerPorVehiculo(Long vehiculoId, Taller taller) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId).orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));
        if (!vehiculo.getTaller().getId().equals(taller.getId())) {
            throw new RuntimeException("Acceso denegado.");
        }
        return ordenRepository.findByVehiculoId(vehiculoId);
    }

    // Metodo para cambiar el estado de una orden
    public OrdenTrabajo actualizarEstado(Long id, EstadoOrden nuevoEstado, Taller taller) {
        OrdenTrabajo orden = ordenRepository.findById(id).orElseThrow(()->new RuntimeException("Orden no encontrada"));
        if (!orden.getTaller().getId().equals(taller.getId())) {
            throw new RuntimeException("Acceso denegado: La orden no pertenece a tu taller.");
        }
        orden.setEstado(nuevoEstado);
        OrdenTrabajo guardada = ordenRepository.save(orden);

        // Disparar email si el vehículo está listo y el cliente tiene email
        if (nuevoEstado == EstadoOrden.FINALIZADO || nuevoEstado == EstadoOrden.ENTREGADO) {
            String emailCliente = orden.getVehiculo().getCliente().getEmail();
            if (emailCliente != null && !emailCliente.isEmpty()) {
                String patente = orden.getVehiculo().getPatente();
                String nombreTaller = orden.getTaller().getNombre();
                String asunto = "¡Tu vehículo está listo! - " + nombreTaller;
                String cuerpo = "Hola " + orden.getVehiculo().getCliente().getNombreCliente() + ",\n\n" +
                        "Te avisamos que tu vehículo (Patente: " + patente + ") ya se encuentra " + 
                        (nuevoEstado == EstadoOrden.FINALIZADO ? "finalizado y listo para retirar" : "entregado") + ".\n\n" +
                        "Cualquier duda, comunicate con nosotros.\n\n" +
                        "Saludos,\nEl equipo de " + nombreTaller;
                
                CompletableFuture.runAsync(() -> {
                    try {
                        emailService.enviarEmail(emailCliente, asunto, cuerpo);
                    } catch (Exception e) {
                        System.err.println("Error enviando email a cliente: " + e.getMessage());
                    }
                });
            }
        }

        return guardada;
    }

    // Metodo para actualizar el presupuesto de una orden de trabajo
    public OrdenTrabajo actualizarCosto(Long id, Double nuevoCosto, Taller taller){
        OrdenTrabajo orden = ordenRepository.findById(id).orElseThrow(()->new RuntimeException("Orden no encontrada"));
        if (!orden.getTaller().getId().equals(taller.getId())) {
            throw new RuntimeException("Acceso denegado: La orden no pertenece a tu taller.");
        }
        orden.setCostoTotal(nuevoCosto);
        return ordenRepository.save(orden);
    }
}