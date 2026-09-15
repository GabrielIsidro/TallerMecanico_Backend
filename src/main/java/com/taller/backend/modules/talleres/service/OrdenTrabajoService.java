package com.taller.backend.modules.talleres.service;
import com.taller.backend.core.service.EmailService;

import com.taller.backend.modules.talleres.model.*;
import com.taller.backend.modules.backoffice.model.*;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import com.taller.backend.modules.talleres.dto.NuevaOrdenRequest;

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

    @Transactional
    public OrdenTrabajo crearOrden(NuevaOrdenRequest request) {
        
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setVehiculo(vehiculo);
        orden.setDescripcion(request.getDescripcion());
        orden.setObservacionesMecanico(request.getObservacionesMecanico());
        orden.setObservacionesCliente(request.getObservacionesCliente());
        orden.setChecklist(request.getChecklist());
        orden.setEstado(EstadoOrden.PENDIENTE);
        
        if (request.getItems() != null) {
            for (ItemOrden item : request.getItems()) {
                if(item.getTipoServicio() != null && item.getTipoServicio().getId() != null) {
                    TipoServicio servicioDb = tipoServicioRepository.findById(item.getTipoServicio().getId())
                            .orElseThrow(() -> new RuntimeException ("Servicio no encontrado"));
                    item.setTipoServicio(servicioDb);
                    item.calcularSubtotal();
                }
                orden.agregarItem(item);
            }
        }

        if (request.getKilometraje() != null) {
            vehiculo.setKilometraje(request.getKilometraje());
            vehiculoRepository.save(vehiculo);
        }

        return ordenRepository.save(orden);
    }

    public Page<OrdenTrabajo> obtenerTodas(Pageable pageable) {
        return ordenRepository.findAll(pageable);
    }

    public OrdenTrabajo guardarOrden(OrdenTrabajo orden) {
        if (orden.getVehiculo() != null && orden.getVehiculo().getId() != null) {
            Vehiculo vehiculoReal = vehiculoRepository.findById(orden.getVehiculo().getId()).orElse(null);
            if (vehiculoReal != null) {
                orden.setVehiculo(vehiculoReal);
            }
        }
        return ordenRepository.save(orden);
    }

    public OrdenTrabajo actualizarPago(Long id, FormaPago formaPago) {
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setFormaPago(formaPago);
        return ordenRepository.save(orden);
    }
    
    public List<OrdenTrabajo> obtenerPorVehiculo(Long vehiculoId) {
        return ordenRepository.findByVehiculoId(vehiculoId);
    }

    public OrdenTrabajo actualizarEstado(Long id, EstadoOrden nuevoEstado) {
        OrdenTrabajo orden = ordenRepository.findById(id).orElseThrow(()->new RuntimeException("Orden no encontrada"));
        orden.setEstado(nuevoEstado);
        OrdenTrabajo guardada = ordenRepository.save(orden);

        if (nuevoEstado == EstadoOrden.FINALIZADO || nuevoEstado == EstadoOrden.ENTREGADO) {
            String emailCliente = orden.getVehiculo().getCliente().getEmail();
            if (emailCliente != null && !emailCliente.isEmpty()) {
                String patente = orden.getVehiculo().getPatente();
                String nombreTaller = orden.getTaller() != null ? orden.getTaller().getNombre() : "Tu Taller";
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

    public OrdenTrabajo actualizarCosto(Long id, Double nuevoCosto){
        OrdenTrabajo orden = ordenRepository.findById(id).orElseThrow(()->new RuntimeException("Orden no encontrada"));
        orden.setCostoTotal(nuevoCosto);
        return ordenRepository.save(orden);
    }

    @Transactional
    public OrdenTrabajo actualizarOrdenCompleta(Long id, NuevaOrdenRequest request) {
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (request.getDescripcion() != null) orden.setDescripcion(request.getDescripcion());
        if (request.getObservacionesMecanico() != null) orden.setObservacionesMecanico(request.getObservacionesMecanico());
        if (request.getObservacionesCliente() != null) orden.setObservacionesCliente(request.getObservacionesCliente());
        if (request.getChecklist() != null) orden.setChecklist(request.getChecklist());

        if (request.getItems() != null) {
            orden.getItems().clear();
            for (ItemOrden item : request.getItems()) {
                if(item.getTipoServicio() != null && item.getTipoServicio().getId() != null) {
                    TipoServicio servicioDb = tipoServicioRepository.findById(item.getTipoServicio().getId())
                            .orElseThrow(() -> new RuntimeException ("Servicio no encontrado"));
                    item.setTipoServicio(servicioDb);
                    item.calcularSubtotal();
                }
                orden.agregarItem(item);
            }
        }

        orden.setCostoTotal(orden.getTotal());

        if (request.getKilometraje() != null) {
            Vehiculo vehiculo = orden.getVehiculo();
            vehiculo.setKilometraje(request.getKilometraje());
            vehiculoRepository.save(vehiculo);
        }

        return ordenRepository.save(orden);
    }
}
