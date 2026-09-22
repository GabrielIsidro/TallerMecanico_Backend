package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.core.service.EmailService;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.dto.NuevaOrdenRequest;
import com.taller.backend.modules.talleres.model.*;
import com.taller.backend.modules.talleres.repository.OrdenTrabajoRepository;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenTrabajoServiceTest {

    @Mock
    private OrdenTrabajoRepository ordenRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private TipoServicioRepository tipoServicioRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private OrdenTrabajoService ordenService;

    private Taller tallerMock;
    private Vehiculo vehiculoMock;
    private OrdenTrabajo ordenMock;

    @BeforeEach
    void setUp() {
        tallerMock = new Taller();
        tallerMock.setId(1L);
        tallerMock.setNombre("Taller Central");

        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setNombreCliente("Martin");
        cliente.setEmail("martin@test.com");
        cliente.setTaller(tallerMock);

        vehiculoMock = new Vehiculo();
        vehiculoMock.setId(20L);
        vehiculoMock.setPatente("AA111BB");
        vehiculoMock.setTaller(tallerMock);
        vehiculoMock.setTallerId(1L);
        vehiculoMock.setCliente(cliente);

        ordenMock = new OrdenTrabajo();
        ordenMock.setId(50L);
        ordenMock.setVehiculo(vehiculoMock);
        ordenMock.setTaller(tallerMock);
        ordenMock.setTallerId(1L);
        ordenMock.setEstado(EstadoOrden.PENDIENTE);
        ordenMock.setCostoTotal(1500.0);
    }

    @Test
    @DisplayName("Debe crear una orden y vincularla al taller autenticado")
    void debeCrearOrdenExitosamente() {
        NuevaOrdenRequest request = new NuevaOrdenRequest();
        request.setVehiculoId(20L);
        request.setDescripcion("Cambio de aceite y filtro");
        request.setItems(new ArrayList<>());

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(vehiculoRepository.findById(20L)).thenReturn(Optional.of(vehiculoMock));
        when(ordenRepository.save(any(OrdenTrabajo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdenTrabajo creada = ordenService.crearOrden(request);

        assertNotNull(creada);
        assertEquals(tallerMock, creada.getTaller());
        assertEquals(1L, creada.getTallerId());
        assertEquals(EstadoOrden.PENDIENTE, creada.getEstado());
        assertEquals("Cambio de aceite y filtro", creada.getDescripcion());
    }

    @Test
    @DisplayName("Debe rechazar la creación de una orden si el vehículo es de otro taller")
    void debeRechazarCrearOrdenConVehiculoAjeno() {
        Taller otroTaller = new Taller();
        otroTaller.setId(99L);
        vehiculoMock.setTaller(otroTaller);

        NuevaOrdenRequest request = new NuevaOrdenRequest();
        request.setVehiculoId(20L);
        request.setDescripcion("Alineación");

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(vehiculoRepository.findById(20L)).thenReturn(Optional.of(vehiculoMock));

        assertThrows(UnauthorizedAccessException.class, () -> ordenService.crearOrden(request));
    }

    @Test
    @DisplayName("Debe actualizar el estado de una orden a FINALIZADO")
    void debeActualizarEstadoAFinalizado() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(ordenRepository.findById(50L)).thenReturn(Optional.of(ordenMock));
        when(ordenRepository.save(any(OrdenTrabajo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdenTrabajo actualizada = ordenService.actualizarEstado(50L, EstadoOrden.FINALIZADO);

        assertNotNull(actualizada);
        assertEquals(EstadoOrden.FINALIZADO, actualizada.getEstado());
    }

    @Test
    @DisplayName("Debe actualizar el costo de una orden")
    void debeActualizarCosto() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(ordenRepository.findById(50L)).thenReturn(Optional.of(ordenMock));
        when(ordenRepository.save(any(OrdenTrabajo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrdenTrabajo actualizada = ordenService.actualizarCosto(50L, 25000.0);

        assertNotNull(actualizada);
        assertEquals(25000.0, actualizada.getCostoTotal());
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si la orden no existe")
    void debeLanzarResourceNotFoundExceptionSiOrdenNoExiste() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(ordenRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ordenService.obtenerPorId(999L));
    }
}
