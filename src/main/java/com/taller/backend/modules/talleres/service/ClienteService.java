package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SecurityHelper securityHelper;

    /**
     * Obtiene los clientes paginados del taller autenticado, con filtro de búsqueda opcional por nombre.
     */
    public Page<Cliente> obtenerClientesPaginados(Pageable pageable, String search) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        if (search != null && !search.trim().isEmpty()) {
            return clienteRepository.findByNombreClienteContainingIgnoreCaseAndTaller(search.trim(), miTaller, pageable);
        }
        return clienteRepository.findByTaller(miTaller, pageable);
    }

    /**
     * Obtiene todos los clientes del taller autenticado sin paginar.
     */
    public List<Cliente> obtenerTodos() {
        Taller miTaller = securityHelper.getTallerAutenticado();
        return clienteRepository.findByTaller(miTaller);
    }

    /**
     * Obtiene un cliente por su ID, validando que pertenezca al taller del usuario autenticado.
     */
    public Cliente obtenerPorId(Long id) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

        if (cliente.getTaller() != null && !cliente.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: el cliente no pertenece a su taller");
        }

        return cliente;
    }

    /**
     * Registra un nuevo cliente vinculándolo obligatoriamente al taller autenticado.
     */
    @Transactional
    public Cliente guardarCliente(Cliente cliente) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        cliente.setTaller(miTaller);
        cliente.setTallerId(miTaller.getId());
        return clienteRepository.save(cliente);
    }

    /**
     * Actualiza los datos de un cliente existente perteneciente al taller autenticado.
     */
    @Transactional
    public Cliente actualizarCliente(Long id, Cliente detalles) {
        Cliente clienteExistente = obtenerPorId(id);

        clienteExistente.setNombreCliente(detalles.getNombreCliente());
        clienteExistente.setTelefono(detalles.getTelefono());
        clienteExistente.setDireccion(detalles.getDireccion());
        clienteExistente.setEmail(detalles.getEmail());
        clienteExistente.setEsEmpresa(detalles.getEsEmpresa());
        clienteExistente.setDocumentoCuit(detalles.getDocumentoCuit());

        return clienteRepository.save(clienteExistente);
    }

    /**
     * Elimina un cliente verificando pertenencia al taller autenticado.
     */
    @Transactional
    public void eliminarCliente(Long id) {
        Cliente cliente = obtenerPorId(id);
        clienteRepository.delete(cliente);
    }
}
