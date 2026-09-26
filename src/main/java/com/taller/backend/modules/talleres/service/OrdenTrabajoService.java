package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.core.service.EmailService;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.dto.ActualizarOrdenRequest;
import com.taller.backend.modules.talleres.dto.NuevaOrdenRequest;
import com.taller.backend.modules.talleres.model.EstadoOrden;
import com.taller.backend.modules.talleres.model.FormaPago;
import com.taller.backend.modules.talleres.model.ItemOrden;
import com.taller.backend.modules.talleres.model.OrdenTrabajo;
import com.taller.backend.modules.talleres.model.TipoServicio;
import com.taller.backend.modules.talleres.model.Vehiculo;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class OrdenTrabajoService {

    @Autowired
    private OrdenTrabajoRepository ordenRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SecurityHelper securityHelper;

    @Transactional
    public OrdenTrabajo crearOrden(NuevaOrdenRequest request) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con id: " + request.getVehiculoId()));

        if (vehiculo.getTaller() != null && !vehiculo.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: el vehículo no pertenece a su taller");
        }

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setTaller(miTaller);
        orden.setTallerId(miTaller.getId());
        orden.setVehiculo(vehiculo);
        orden.setDescripcion(request.getDescripcion());
        orden.setObservacionesMecanico(request.getObservacionesMecanico());
        orden.setObservacionesCliente(request.getObservacionesCliente());
        orden.setChecklist(request.getChecklist());
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setFormaPago(FormaPago.EFECTIVO);
        
        if (request.getItems() != null) {
            for (ItemOrden item : request.getItems()) {
                if (item.getTipoServicio() != null && item.getTipoServicio().getId() != null) {
                    Long servId = item.getTipoServicio().getId();
                    TipoServicio servicioDb = tipoServicioRepository.findById(servId)
                            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servId));
                    item.setTipoServicio(servicioDb);
                    item.calcularSubtotal();
                }
                orden.agregarItem(item);
            }
        }

        orden.setCostoTotal(orden.getTotal());

        if (request.getKilometraje() != null) {
            vehiculo.setKilometraje(request.getKilometraje());
            vehiculoRepository.save(vehiculo);
        }

        return ordenRepository.save(orden);
    }

    public Page<OrdenTrabajo> obtenerTodas(Pageable pageable) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        return ordenRepository.findByTaller(miTaller, pageable);
    }

    @Transactional
    public OrdenTrabajo guardarOrden(OrdenTrabajo orden) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        orden.setTaller(miTaller);
        orden.setTallerId(miTaller.getId());

        if (orden.getVehiculo() != null && orden.getVehiculo().getId() != null) {
            Vehiculo vehiculoReal = vehiculoRepository.findById(orden.getVehiculo().getId()).orElse(null);
            if (vehiculoReal != null) {
                orden.setVehiculo(vehiculoReal);
            }
        }
        return ordenRepository.save(orden);
    }

    @Transactional
    public OrdenTrabajo actualizarPago(Long id, FormaPago formaPago) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        if (orden.getTaller() != null && !orden.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }

        orden.setFormaPago(formaPago);
        return ordenRepository.save(orden);
    }
    
    public List<OrdenTrabajo> obtenerPorVehiculo(Long vehiculoId) {
        return ordenRepository.findByVehiculoId(vehiculoId);
    }

    public OrdenTrabajo obtenerPorId(Long id) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        if (orden.getTaller() != null && !orden.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }

        return orden;
    }

    @Transactional
    public OrdenTrabajo actualizarEstado(Long id, EstadoOrden nuevoEstado) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        if (orden.getTaller() != null && !orden.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }

        orden.setEstado(nuevoEstado);
        OrdenTrabajo guardada = ordenRepository.save(orden);

        if (nuevoEstado == EstadoOrden.FINALIZADO || nuevoEstado == EstadoOrden.ENTREGADO) {
            String emailCliente = orden.getVehiculo() != null && orden.getVehiculo().getCliente() != null ? orden.getVehiculo().getCliente().getEmail() : null;
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

    @Transactional
    public OrdenTrabajo actualizarCosto(Long id, Double nuevoCosto) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        if (orden.getTaller() != null && !orden.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }

        orden.setCostoTotal(nuevoCosto);
        return ordenRepository.save(orden);
    }

    @Transactional
    public OrdenTrabajo actualizarOrdenCompleta(Long id, ActualizarOrdenRequest request) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        if (orden.getTaller() != null && !orden.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }

        if (request.getDescripcion() != null) orden.setDescripcion(request.getDescripcion());
        if (request.getObservacionesMecanico() != null) orden.setObservacionesMecanico(request.getObservacionesMecanico());
        if (request.getObservacionesCliente() != null) orden.setObservacionesCliente(request.getObservacionesCliente());
        if (request.getChecklist() != null) orden.setChecklist(request.getChecklist());

        if (request.getItems() != null) {
            orden.getItems().clear();
            for (ItemOrden item : request.getItems()) {
                if (item.getTipoServicio() != null && item.getTipoServicio().getId() != null) {
                    Long servId = item.getTipoServicio().getId();
                    TipoServicio servicioDb = tipoServicioRepository.findById(servId)
                            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con id: " + servId));
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
