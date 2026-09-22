package com.taller.backend.modules.backoffice.service;

import com.taller.backend.core.exception.BusinessRuleException;
import com.taller.backend.core.exception.DuplicateResourceException;
import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.service.EmailService;
import com.taller.backend.modules.backoffice.dto.TallerRegistroDTO;
import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import com.taller.backend.modules.talleres.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TallerServiceTest {

    @Mock
    private TallerRepository tallerRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private OrdenTrabajoRepository ordenTrabajoRepository;

    @Mock
    private TipoServicioRepository tipoServicioRepository;

    @Mock
    private RepuestoRepository repuestoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TallerService tallerService;

    private Taller tallerMock;

    @BeforeEach
    void setUp() {
        tallerMock = new Taller();
        tallerMock.setId(1L);
        tallerMock.setNombre("Taller El Rayo");
        tallerMock.setTitular("Alberto Perez");
        tallerMock.setEmailContacto("contacto@elrayo.com");
        tallerMock.setEstadoSuscripcion(EstadoSuscripcion.PRUEBA_GRATUITA);
        tallerMock.setFechaVencimiento(LocalDate.now().plusDays(30));
    }

    @Test
    @DisplayName("Debe listar todos los talleres")
    void debeListarTalleres() {
        when(tallerRepository.findAll()).thenReturn(List.of(tallerMock));

        List<Taller> resultado = tallerService.listarTalleres();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Taller El Rayo", resultado.get(0).getNombre());
    }

    @Test
    @DisplayName("Debe crear un taller con prueba gratuita de 30 días e insertar admin")
    void debeCrearTallerExitosamente() {
        TallerRegistroDTO dto = new TallerRegistroDTO();
        dto.setNombre("Taller Nuevo");
        dto.setTitular("Carlos Gómez");
        dto.setEmailContacto("nuevo@taller.com");
        dto.setPassword("secreta123");

        when(usuarioRepository.findByEmail("nuevo@taller.com")).thenReturn(Optional.empty());
        when(tallerRepository.save(any(Taller.class))).thenAnswer(invocation -> {
            Taller t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });
        when(passwordEncoder.encode("secreta123")).thenReturn("encodedPassword");

        Taller creado = tallerService.crearTaller(dto);

        assertNotNull(creado);
        assertEquals(EstadoSuscripcion.PRUEBA_GRATUITA, creado.getEstadoSuscripcion());
        assertEquals(LocalDate.now().plusDays(30), creado.getFechaVencimiento());

        verify(usuarioRepository, times(1)).insertAdminTaller(
                eq("nuevo@taller.com"),
                eq("encodedPassword"),
                anyString(),
                anyString(),
                eq("ADMIN_TALLER"),
                eq(2L)
        );
    }

    @Test
    @DisplayName("Debe rechazar la creación si el email ya existe")
    void debeRechazarCreacionSiEmailYaExiste() {
        TallerRegistroDTO dto = new TallerRegistroDTO();
        dto.setNombre("Taller Duplicado");
        dto.setEmailContacto("existente@taller.com");
        dto.setPassword("123456");

        when(usuarioRepository.findByEmail("existente@taller.com")).thenReturn(Optional.of(new com.taller.backend.modules.talleres.model.Usuario()));

        assertThrows(DuplicateResourceException.class, () -> tallerService.crearTaller(dto));
    }

    @Test
    @DisplayName("Debe eliminar un taller en cascada")
    void debeEliminarTallerEnCascada() {
        when(tallerRepository.existsById(1L)).thenReturn(true);

        tallerService.eliminarTaller(1L);

        verify(ordenTrabajoRepository).deleteByTallerId(1L);
        verify(vehiculoRepository).deleteByTallerId(1L);
        verify(clienteRepository).deleteByTallerId(1L);
        verify(tipoServicioRepository).deleteByTallerId(1L);
        verify(repuestoRepository).deleteByTallerId(1L);
        verify(usuarioRepository).deleteByTallerId(1L);
        verify(tallerRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el taller a eliminar no existe")
    void debeLanzarResourceNotFoundSiTallerNoExiste() {
        when(tallerRepository.existsById(999L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> tallerService.eliminarTaller(999L));
    }
}
