package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.repository.ClienteRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private ClienteService clienteService;

    private Taller tallerMock;
    private Cliente clienteMock;

    @BeforeEach
    void setUp() {
        tallerMock = new Taller();
        tallerMock.setId(1L);
        tallerMock.setNombre("Taller Mecánico Central");

        clienteMock = new Cliente();
        clienteMock.setId(10L);
        clienteMock.setNombreCliente("Carlos Perez");
        clienteMock.setEmail("carlos@gmail.com");
        clienteMock.setTaller(tallerMock);
        clienteMock.setTallerId(1L);
    }

    @Test
    @DisplayName("Debe listar todos los clientes del taller autenticado")
    void debeListarClientesDelTallerAutenticado() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findByTaller(tallerMock)).thenReturn(List.of(clienteMock));

        List<Cliente> resultado = clienteService.obtenerTodos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Carlos Perez", resultado.get(0).getNombreCliente());
        verify(clienteRepository, times(1)).findByTaller(tallerMock);
    }

    @Test
    @DisplayName("Debe obtener clientes paginados con filtro de búsqueda por nombre")
    void debeObtenerClientesPaginadosConFiltro() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> page = new PageImpl<>(List.of(clienteMock));

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findByNombreClienteContainingIgnoreCaseAndTaller("Carlos", tallerMock, pageable))
                .thenReturn(page);

        Page<Cliente> resultado = clienteService.obtenerClientesPaginados(pageable, "Carlos");

        assertNotNull(resultado);
        assertEquals(1, resultado.getTotalElements());
        verify(clienteRepository).findByNombreClienteContainingIgnoreCaseAndTaller("Carlos", tallerMock, pageable);
    }

    @Test
    @DisplayName("Debe obtener un cliente por ID si pertenece al taller autenticado")
    void debeObtenerPorIdExitosamente() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

        Cliente resultado = clienteService.obtenerPorId(10L);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Carlos Perez", resultado.getNombreCliente());
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedAccessException si el cliente pertenece a otro taller")
    void debeLanzarUnauthorizedAccessCuandoClientePerteneceAOtroTaller() {
        Taller otroTaller = new Taller();
        otroTaller.setId(99L);
        otroTaller.setNombre("Taller Ajeno");

        clienteMock.setTaller(otroTaller);

        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));

        assertThrows(UnauthorizedAccessException.class, () -> clienteService.obtenerPorId(10L));
    }

    @Test
    @DisplayName("Debe lanzar ResourceNotFoundException si el cliente no existe")
    void debeLanzarResourceNotFoundExceptionCuandoClienteNoExiste() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clienteService.obtenerPorId(999L));
    }

    @Test
    @DisplayName("Debe guardar un cliente vinculándolo al taller autenticado")
    void debeGuardarClienteConTallerAutenticado() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente nuevo = new Cliente();
        nuevo.setNombreCliente("Ana Gómez");

        Cliente guardado = clienteService.guardarCliente(nuevo);

        assertNotNull(guardado);
        assertEquals(tallerMock, guardado.getTaller());
        assertEquals(1L, guardado.getTallerId());
        verify(clienteRepository).save(nuevo);
    }

    @Test
    @DisplayName("Debe eliminar un cliente si pertenece al taller autenticado")
    void debeEliminarClienteExitosamente() {
        when(securityHelper.getTallerAutenticado()).thenReturn(tallerMock);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(clienteMock));
        doNothing().when(clienteRepository).delete(clienteMock);

        clienteService.eliminarCliente(10L);

        verify(clienteRepository, times(1)).delete(clienteMock);
    }
}
