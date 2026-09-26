package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.model.Vehiculo;
import com.taller.backend.modules.talleres.repository.ClienteRepository;
import com.taller.backend.modules.talleres.repository.VehiculoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Service
public class VehiculoService {
    
    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private SecurityHelper securityHelper;

    /**
     * Obtiene los vehículos paginados del taller autenticado, con filtro de búsqueda opcional por patente.
     */
    public Page<Vehiculo> obtenerVehiculosPaginados(Pageable pageable, String search) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        if (search != null && !search.trim().isEmpty()) {
            return vehiculoRepository.findByPatenteContainingIgnoreCaseAndTaller(search.trim(), miTaller, pageable);
        }
        return vehiculoRepository.findByTaller(miTaller, pageable);
    }

    /**
     * Obtiene todos los vehículos pertenecientes al taller autenticado.
     */
    public List<Vehiculo> getAllVehiculos() { 
        Taller miTaller = securityHelper.getTallerAutenticado();
        return vehiculoRepository.findByTaller(miTaller); 
    }

    /**
     * Obtiene un vehículo por ID verificando que pertenezca al taller autenticado.
     */
    public Vehiculo getVehiculoById(Long id) { 
        Taller miTaller = securityHelper.getTallerAutenticado();
        Vehiculo vehiculo = vehiculoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con id: " + id));

        if (vehiculo.getTaller() != null && !vehiculo.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: el vehículo no pertenece a su taller");
        }

        return vehiculo;
    }

    /**
     * Guarda un vehículo asociándolo obligatoriamente al taller autenticado
     * y validando la pertenencia del cliente si viene asignado.
     */
    @Transactional
    public Vehiculo guardarVehiculo(Vehiculo vehiculo) {
        Taller miTaller = securityHelper.getTallerAutenticado();

        // 1. Validar cliente si fue provisto
        if (vehiculo.getCliente() != null && vehiculo.getCliente().getId() != null) {
            Long clienteId = vehiculo.getCliente().getId();
            Cliente clienteReal = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));

            if (clienteReal.getTaller() != null && !clienteReal.getTaller().getId().equals(miTaller.getId())) {
                throw new UnauthorizedAccessException("Acceso denegado: el cliente especificado no pertenece a su taller");
            }
            vehiculo.setCliente(clienteReal);
        }

        // 2. Asociar al taller actual
        vehiculo.setTaller(miTaller);
        vehiculo.setTallerId(miTaller.getId());

        return vehiculoRepository.save(vehiculo);
    }

    /**
     * Actualiza un vehículo existente verificando pertenencia al taller autenticado.
     */
    @Transactional
    public Vehiculo actualizarVehiculo(Long id, Vehiculo detalles) {
        Vehiculo vehiculoExistente = getVehiculoById(id);
        Taller miTaller = securityHelper.getTallerAutenticado();

        vehiculoExistente.setPatente(detalles.getPatente());
        vehiculoExistente.setModelo(detalles.getModelo());
        vehiculoExistente.setMarca(detalles.getMarca());
        vehiculoExistente.setColor(detalles.getColor());
        vehiculoExistente.setAnio(detalles.getAnio());
        vehiculoExistente.setNumeroMotor(detalles.getNumeroMotor());
        vehiculoExistente.setNumeroChasis(detalles.getNumeroChasis());
        vehiculoExistente.setKilometraje(detalles.getKilometraje());
        vehiculoExistente.setProximoServiceKm(detalles.getProximoServiceKm());

        if (detalles.getCliente() != null && detalles.getCliente().getId() != null) {
            Long clienteId = detalles.getCliente().getId();
            Cliente clienteReal = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId));

            if (clienteReal.getTaller() != null && !clienteReal.getTaller().getId().equals(miTaller.getId())) {
                throw new UnauthorizedAccessException("Acceso denegado: el cliente especificado no pertenece a su taller");
            }
            vehiculoExistente.setCliente(clienteReal);
        }

        return vehiculoRepository.save(vehiculoExistente);
    }

    /**
     * Elimina un vehículo por ID verificando pertenencia al taller autenticado.
     */
    @Transactional
    public void deleteVehiculo(Long id) { 
        Vehiculo vehiculo = getVehiculoById(id);
        vehiculoRepository.delete(vehiculo); 
    }
}
