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
}
