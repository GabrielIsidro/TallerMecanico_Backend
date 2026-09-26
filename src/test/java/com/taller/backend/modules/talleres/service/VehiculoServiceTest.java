package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.model.Vehiculo;
import com.taller.backend.modules.talleres.repository.ClienteRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private VehiculoService vehiculoService;

    private Taller tallerMock;
    private Cliente clienteMock;
    private Vehiculo vehiculoMock;

    @BeforeEach
    void setUp() {
        tallerMock = new Taller();
        tallerMock.setId(1L);
        tallerMock.setNombre("Taller Mecánico Central");

        clienteMock = new Cliente();
        clienteMock.setId(5L);
        clienteMock.setNombreCliente("Juan Lopez");
        clienteMock.setTaller(tallerMock);
        clienteMock.setTallerId(1L);

        vehiculoMock = new Vehiculo();
        vehiculoMock.setId(100L);
        vehiculoMock.setPatente("AA123BB");
        vehiculoMock.setMarca("Ford");
        vehiculoMock.setModelo("Focus");
        vehiculoMock.setTaller(tallerMock);
        vehiculoMock.setTallerId(1L);
        vehiculoMock.setCliente(clienteMock);
    }

    @Test
    @DisplayName("Debe listar todos los vehículos del taller autenticado")
    void debeListarVehiculosDelTaller() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(vehiculoRepository.findByTaller(tallerMock)).thenReturn(List.of(vehiculoMock));

        List<Vehiculo> resultado = vehiculoService.getAllVehiculos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("AA123BB", resultado.get(0).getPatente());
    }

    @Test
    @DisplayName("Debe obtener vehículos paginados por patente")
    void debeObtenerVehiculosPaginadosPorPatente() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vehiculo> page = new PageImpl<>(List.of(vehiculoMock));

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(vehiculoRepository.findByPatenteContainingIgnoreCaseAndTaller("AA", tallerMock, pageable))
                .thenReturn(page);

        Page<Vehiculo> resultado = vehiculoService.obtenerVehiculosPaginados(pageable, "AA");

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        verify(vehiculoRepository).findByPatenteContainingIgnoreCaseAndTaller("AA", tallerMock, pageable);
    }

    @Test
    @DisplayName("Debe obtener un vehículo por ID si pertenece al taller")
    void debeObtenerVehiculoPorIdExitosamente() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(vehiculoRepository.findById(100L)).thenReturn(Optional.of(vehiculoMock));

        Vehiculo resultado = vehiculoService.getVehiculoById(100L);

        assertNotNull(resultado);
        assertEquals("AA123BB", resultado.getPatente());
    }

    @Test
    @DisplayName("Debe rechazar la consulta de un vehículo que pertenece a otro taller")
    void debeRechazarVehiculoDeOtroTaller() {
        Taller otroTaller = new Taller();
        otroTaller.setId(99L);
        vehiculoMock.setTaller(otroTaller);

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(vehiculoRepository.findById(100L)).thenReturn(Optional.of(vehiculoMock));

        assertThrows(UnauthorizedAccessException.class, () -> vehiculoService.getVehiculoById(100L));
    }

    @Test
    @DisplayName("Debe guardar vehículo y vincularlo al taller autenticado")
    void debeGuardarVehiculoConTaller() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(clienteMock));
        when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehiculo nuevo = new Vehiculo();
        nuevo.setPatente("CC999ZZ");
        nuevo.setCliente(clienteMock);

        Vehiculo guardado = vehiculoService.guardarVehiculo(nuevo);

        assertNotNull(guardado);
        assertEquals(tallerMock, guardado.getTaller());
        assertEquals(1L, guardado.getTallerId());
        verify(vehiculoRepository).save(nuevo);
    }

    @Test
    @DisplayName("Debe rechazar la asignación de un cliente que pertenece a otro taller")
    void debeRechazarClienteDeOtroTaller() {
        Taller otroTaller = new Taller();
        otroTaller.setId(99L);
        clienteMock.setTaller(otroTaller);

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(clienteMock));

        Vehiculo nuevo = new Vehiculo();
        nuevo.setPatente("CC999ZZ");
        nuevo.setCliente(clienteMock);

        assertThrows(UnauthorizedAccessException.class, () -> vehiculoService.guardarVehiculo(nuevo));
    }
}
