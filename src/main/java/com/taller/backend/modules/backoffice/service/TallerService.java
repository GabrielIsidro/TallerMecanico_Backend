package com.taller.backend.modules.backoffice.service;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.DuplicateResourceException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.service.EmailService;
import com.taller.backend.modules.backoffice.dto.TallerRegistroDTO;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.model.RolUsuario;
import com.taller.backend.modules.talleres.model.Usuario;
import com.taller.backend.modules.talleres.repository.*;
import com.taller.backend.modules.backoffice.model.FrecuenciaPlan;
import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import com.taller.backend.modules.backoffice.model.TipoPlan;
import com.taller.backend.modules.backoffice.repository.PlanSuscripcionRepository;
import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.model.EstadoOrden;
import com.taller.backend.modules.talleres.model.OrdenTrabajo;
import com.taller.backend.modules.talleres.model.Vehiculo;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TallerService {

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private PlanSuscripcionRepository planSuscripcionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Transactional(readOnly = true)
    public List<Taller> listarTalleres() {
        return tallerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Taller obtenerPorId(Long id) {
        return tallerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Taller no encontrado con id: " + id));
    }

    @Transactional
    public Taller crearTaller(TallerRegistroDTO dto) {
        if (dto.getEmailContacto() == null || dto.getEmailContacto().trim().isEmpty()) {
            throw new BusinessRuleException("El email de contacto es obligatorio.");
        }

        String email = dto.getEmailContacto().trim();
        if (usuarioRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("El email ya está en uso.");
        }

        Taller taller = new Taller();
        taller.setNombre(dto.getNombre());
        taller.setTitular(dto.getTitular());
        taller.setTelefono(dto.getTelefono());
        taller.setEmailContacto(email);
        taller.setEstadoSuscripcion(EstadoSuscripcion.PRUEBA_GRATUITA);
        taller.setFechaVencimiento(LocalDate.now().plusDays(30));

        Taller tallerGuardado = tallerRepository.save(taller);

        String nombreAdmin = (dto.getNombreAdmin() != null && !dto.getNombreAdmin().trim().isEmpty())
                ? dto.getNombreAdmin().trim()
                : (dto.getTitular() != null && !dto.getTitular().trim().isEmpty() ? dto.getTitular().trim() : "Admin");

        String apellidoAdmin = (dto.getApellidoAdmin() != null && !dto.getApellidoAdmin().trim().isEmpty())
                ? dto.getApellidoAdmin().trim()
                : (dto.getNombre() != null ? dto.getNombre() : "");

        String passwordTemporal = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                ? dto.getPassword().trim()
                : generarPasswordAleatoria();

        try {
            usuarioRepository.insertAdminTaller(
                    email,
                    passwordEncoder.encode(passwordTemporal),
                    nombreAdmin,
                    apellidoAdmin,
                    RolUsuario.ADMIN_TALLER.name(),
                    tallerGuardado.getId(),
                    true
            );
        } catch (Exception e) {
            throw new BusinessRuleException("Error al crear el administrador del taller: " + e.getMessage());
        }

        try {
            emailService.enviarEmailBienvenida(email, tallerGuardado.getNombre(), passwordTemporal);
        } catch (Exception e) {
            System.err.println("Aviso: No se pudo enviar el email de bienvenida: " + e.getMessage());
        }

        return tallerGuardado;
    }

    private String generarPasswordAleatoria() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public Taller actualizarSuscripcion(Long id, EstadoSuscripcion estado, Integer diasExtension) {
        Taller taller = obtenerPorId(id);
        taller.setEstadoSuscripcion(estado);
        if (diasExtension != null) {
            LocalDate base = (taller.getFechaVencimiento() != null && taller.getFechaVencimiento().isAfter(LocalDate.now()))
                    ? taller.getFechaVencimiento()
                    : LocalDate.now();
            taller.setFechaVencimiento(base.plusDays(diasExtension));
        }
        return tallerRepository.save(taller);
    }

    @Transactional
    public void eliminarTaller(Long id) {
        if (!tallerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Taller no encontrado con id: " + id);
        }

        ordenTrabajoRepository.deleteByTallerId(id);
        vehiculoRepository.deleteByTallerId(id);
        clienteRepository.deleteByTallerId(id);
        tipoServicioRepository.deleteByTallerId(id);
        repuestoRepository.deleteByTallerId(id);
        usuarioRepository.deleteByTallerId(id);
        tallerRepository.deleteById(id);
    }

    @Transactional
    public void eliminarUsuarioPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún usuario con el correo: " + email));
        usuarioRepository.delete(usuario);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMetricasGlobales() {
        List<Taller> talleres = tallerRepository.findAll();
        long totalTalleres = talleres.size();
        long activas = 0;
        long prueba = 0;
        long vencidas = 0;
        long suspendidas = 0;

        List<PlanSuscripcion> planes = planSuscripcionRepository.findAll();
        double precioBaseMensual = planes.stream()
                .filter(p -> p.getTipo() == TipoPlan.BASE && p.getFrecuencia() == FrecuenciaPlan.MENSUAL && Boolean.TRUE.equals(p.getActivo()))
                .map(PlanSuscripcion::getPrecio)
                .findFirst().orElse(19000.0);
        double precioProMensual = planes.stream()
                .filter(p -> p.getTipo() == TipoPlan.PRO && p.getFrecuencia() == FrecuenciaPlan.MENSUAL && Boolean.TRUE.equals(p.getActivo()))
                .map(PlanSuscripcion::getPrecio)
                .findFirst().orElse(39000.0);

        double mrrEstimado = 0.0;

        for (Taller t : talleres) {
            if (t.getEstadoSuscripcion() == EstadoSuscripcion.ACTIVA) {
                activas++;
                mrrEstimado += (t.getTipoPlan() == TipoPlan.PRO) ? precioProMensual : precioBaseMensual;
            } else if (t.getEstadoSuscripcion() == EstadoSuscripcion.PRUEBA_GRATUITA) {
                prueba++;
            } else if (t.getEstadoSuscripcion() == EstadoSuscripcion.VENCIDA) {
                vencidas++;
            } else if (t.getEstadoSuscripcion() == EstadoSuscripcion.SUSPENDIDA) {
                suspendidas++;
            }
        }

        long totalVehiculos = vehiculoRepository.count();
        long totalOrdenes = ordenTrabajoRepository.count();
        long totalClientes = clienteRepository.count();
        long totalUsuarios = usuarioRepository.count();

        Map<String, Object> metricas = new HashMap<>();
        metricas.put("totalTalleres", totalTalleres);
        metricas.put("talleresActivos", activas);
        metricas.put("talleresPrueba", prueba);
        metricas.put("talleresVencidos", vencidas);
        metricas.put("talleresSuspendidos", suspendidas);
        metricas.put("mrrEstimado", mrrEstimado);
        metricas.put("totalVehiculos", totalVehiculos);
        metricas.put("totalOrdenes", totalOrdenes);
        metricas.put("totalClientes", totalClientes);
        metricas.put("totalUsuarios", totalUsuarios);

        return metricas;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> obtenerMonitoreoTaller(Long id) {
        Taller taller = obtenerPorId(id);

        List<Vehiculo> vehiculos = vehiculoRepository.findByTallerId(id);
        List<Cliente> clientes = clienteRepository.findByTallerId(id);
        List<Usuario> usuarios = usuarioRepository.findByTallerId(id);
        List<OrdenTrabajo> ordenes = ordenTrabajoRepository.findByTallerId(id);

        long enReparacion = ordenes.stream()
                .filter(o -> o.getEstado() != null && o.getEstado() == EstadoOrden.EN_REPARACION)
                .count();
        long finalizadas = ordenes.stream()
                .filter(o -> o.getEstado() != null && (o.getEstado() == EstadoOrden.FINALIZADO || o.getEstado() == EstadoOrden.ENTREGADO))
                .count();
        long presupuestadas = ordenes.stream()
                .filter(o -> o.getEstado() != null && (o.getEstado() == EstadoOrden.PRESUPUESTADO || o.getEstado() == EstadoOrden.PENDIENTE))
                .count();

        double facturacionHistorica = ordenes.stream()
                .filter(o -> o.getCostoTotal() != null)
                .mapToDouble(OrdenTrabajo::getCostoTotal)
                .sum();

        long diasRestantes = 0;
        if (taller.getFechaVencimiento() != null) {
            diasRestantes = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), taller.getFechaVencimiento());
        }

        List<Map<String, Object>> usuariosResumen = usuarios.stream().map(u -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", u.getId());
            userMap.put("nombre", u.getNombre() + (u.getApellido() != null && !u.getApellido().isEmpty() ? " " + u.getApellido() : ""));
            userMap.put("email", u.getEmail());
            userMap.put("rol", u.getRol() != null ? u.getRol().name() : "MECANICO");
            return userMap;
        }).collect(Collectors.toList());

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("taller", taller);
        resultado.put("diasRestantes", diasRestantes);
        resultado.put("totalVehiculos", vehiculos.size());
        resultado.put("totalClientes", clientes.size());
        resultado.put("totalUsuarios", usuarios.size());
        resultado.put("totalOrdenes", ordenes.size());
        resultado.put("ordenesEnReparacion", enReparacion);
        resultado.put("ordenesFinalizadas", finalizadas);
        resultado.put("ordenesPresupuestadas", presupuestadas);
        resultado.put("facturacionHistorica", facturacionHistorica);
        resultado.put("usuarios", usuariosResumen);

        return resultado;
    }
}
