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
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.model.Vehiculo;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import com.taller.backend.modules.talleres.repository.UsuarioRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.taller.backend.modules.talleres.dto.RepuestoOrdenRequest;
import com.taller.backend.modules.talleres.model.RepuestoOrden;
import com.taller.backend.modules.talleres.model.TipoMovimiento;
import com.taller.backend.modules.talleres.model.RolUsuario;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SecurityHelper securityHelper;

    @Autowired
    private RepuestoService repuestoService;

    @Transactional
    public OrdenTrabajo crearOrden(NuevaOrdenRequest request) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con id: " + request.getVehiculoId()));

        if (vehiculo.getTaller() != null && !vehiculo.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: el vehículo no pertenece a su taller");
        }
        if (vehiculo.getTallerId() != null && !vehiculo.getTallerId().equals(miTaller.getId())) {
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

        if (request.getMecanicoAsignadoId() != null) {
            Usuario mecanico = usuarioRepository.findById(request.getMecanicoAsignadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mecánico no encontrado con id: " + request.getMecanicoAsignadoId()));
            validarPertenenciaMecanico(mecanico, miTaller);
            orden.setMecanicoAsignado(mecanico);
        } else {
            // Si no se asignó explícitamente y quien crea la orden es un mecánico, auto-asignarlo
            org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                String email = auth.getName();
                Usuario usuarioActual = usuarioRepository.findByEmail(email).orElse(null);
                if (usuarioActual != null && usuarioActual.getRol() == RolUsuario.MECANICO) {
                    orden.setMecanicoAsignado(usuarioActual);
                }
            }
        }
        
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

        OrdenTrabajo guardada = ordenRepository.save(orden);

        if (request.getRepuestos() != null && !request.getRepuestos().isEmpty()) {
            sincronizarStockRepuestos(guardada, request.getRepuestos());
            guardada.setCostoTotal(guardada.getTotal());
            guardada = ordenRepository.save(guardada);
        }

        return guardada;
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

        validarPertenenciaTaller(orden, miTaller);

        orden.setFormaPago(formaPago);
        return ordenRepository.save(orden);
    }
    
    @Transactional(readOnly = true)
    public List<OrdenTrabajo> obtenerPorVehiculo(Long vehiculoId) {
        return ordenRepository.findByVehiculoId(vehiculoId);
    }

    @Transactional(readOnly = true)
    public OrdenTrabajo obtenerPorId(Long id) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        validarPertenenciaTaller(orden, miTaller);

        return orden;
    }

    @Transactional
    public OrdenTrabajo actualizarEstado(Long id, EstadoOrden nuevoEstado) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        validarPertenenciaTaller(orden, miTaller);

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

        validarPertenenciaTaller(orden, miTaller);

        orden.setCostoTotal(nuevoCosto);
        return ordenRepository.save(orden);
    }

    @Transactional
    public OrdenTrabajo actualizarOrdenCompleta(Long id, ActualizarOrdenRequest request) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));

        validarPertenenciaTaller(orden, miTaller);

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

        if (request.getRepuestos() != null) {
            sincronizarStockRepuestos(orden, request.getRepuestos());
        }

        orden.setCostoTotal(orden.getTotal());

        if (request.getKilometraje() != null) {
            Vehiculo vehiculo = orden.getVehiculo();
            vehiculo.setKilometraje(request.getKilometraje());
            vehiculoRepository.save(vehiculo);
        }

        if (request.getMecanicoAsignadoId() != null) {
            Usuario mecanico = usuarioRepository.findById(request.getMecanicoAsignadoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mecánico no encontrado con id: " + request.getMecanicoAsignadoId()));
            validarPertenenciaMecanico(mecanico, miTaller);
            orden.setMecanicoAsignado(mecanico);
        }

        return ordenRepository.save(orden);
    }

    private void sincronizarStockRepuestos(OrdenTrabajo orden, List<RepuestoOrdenRequest> nuevosRepuestosReq) {
        if (nuevosRepuestosReq == null) return;

        Map<Long, Integer> prevConsumed = new HashMap<>();
        if (orden.getRepuestos() != null) {
            for (RepuestoOrden ro : orden.getRepuestos()) {
                if (ro.getRepuestoId() != null) {
                    int cant = ro.getCantidad() != null && ro.getCantidad() > 0 ? ro.getCantidad() : 1;
                    prevConsumed.put(ro.getRepuestoId(), prevConsumed.getOrDefault(ro.getRepuestoId(), 0) + cant);
                }
            }
        }

        Map<Long, Integer> newConsumed = new HashMap<>();
        for (RepuestoOrdenRequest req : nuevosRepuestosReq) {
            if (req.getRepuestoId() != null) {
                int cant = req.getCantidad() != null && req.getCantidad() > 0 ? req.getCantidad() : 1;
                newConsumed.put(req.getRepuestoId(), newConsumed.getOrDefault(req.getRepuestoId(), 0) + cant);
            }
        }

        Set<Long> allRepuestoIds = new HashSet<>();
        allRepuestoIds.addAll(prevConsumed.keySet());
        allRepuestoIds.addAll(newConsumed.keySet());

        for (Long repId : allRepuestoIds) {
            int prev = prevConsumed.getOrDefault(repId, 0);
            int next = newConsumed.getOrDefault(repId, 0);
            int diff = next - prev;

            if (diff > 0) {
                repuestoService.ajustarStock(
                        repId,
                        TipoMovimiento.SALIDA,
                        diff,
                        "Consumido en Orden #" + orden.getId(),
                        orden.getId()
                );
            } else if (diff < 0) {
                repuestoService.ajustarStock(
                        repId,
                        TipoMovimiento.ENTRADA,
                        Math.abs(diff),
                        "Reintegrado de Orden #" + orden.getId(),
                        orden.getId()
                );
            }
        }

        orden.getRepuestos().clear();
        for (RepuestoOrdenRequest req : nuevosRepuestosReq) {
            RepuestoOrden ro = new RepuestoOrden();
            ro.setDescripcion(req.getDescripcion());
            ro.setProveedor(req.getProveedor());
            ro.setCantidad(req.getCantidad() != null && req.getCantidad() > 0 ? req.getCantidad() : 1);
            ro.setPrecioUnitario(req.getPrecioUnitario());
            ro.setCostoUnitario(req.getCostoUnitario());
            ro.setRepuestoId(req.getRepuestoId());
            ro.calcularSubtotales();
            orden.agregarRepuesto(ro);
        }
    }

    @Transactional
    public OrdenTrabajo asignarMecanico(Long ordenId, Long mecanicoId) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        OrdenTrabajo orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + ordenId));

        validarPertenenciaTaller(orden, miTaller);

        if (mecanicoId != null) {
            Usuario mecanico = usuarioRepository.findById(mecanicoId)
                    .orElseThrow(() -> new ResourceNotFoundException("Mecánico no encontrado con id: " + mecanicoId));
            validarPertenenciaMecanico(mecanico, miTaller);
            orden.setMecanicoAsignado(mecanico);
        } else {
            orden.setMecanicoAsignado(null);
        }

        return ordenRepository.save(orden);
    }

    private void validarPertenenciaTaller(OrdenTrabajo orden, Taller miTaller) {
        if (orden.getTaller() != null && !orden.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }
        if (orden.getTallerId() != null && !orden.getTallerId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: la orden no pertenece a su taller");
        }
    }

    private void validarPertenenciaMecanico(Usuario mecanico, Taller miTaller) {
        Long tallerMecId = mecanico.getTallerId() != null ? mecanico.getTallerId() : (mecanico.getTaller() != null ? mecanico.getTaller().getId() : null);
        if (tallerMecId == null || !tallerMecId.equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("El mecánico no pertenece a este taller");
        }
    }
}
