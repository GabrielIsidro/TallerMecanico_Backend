package com.taller.backend.service;

import com.taller.backend.model.Taller;
import com.taller.backend.repository.TallerRepository;
import com.taller.backend.model.Vehiculo;
import com.taller.backend.repository.ClienteRepository;
import com.taller.backend.model.Cliente;
import com.taller.backend.repository.VehiculoRepository;
import com.taller.backend.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class VehiculoService {
    
    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private SecurityHelper securityHelper;

    // Obtener todos los vehiculos
    public List<Vehiculo> getAllVehiculos() { 
        // ---> CAMBIO: Trae solo los autos del Taller Autenticado
        return vehiculoRepository.findByTallerId(securityHelper.getTallerAutenticado().getId()); 
    }

    // Obtener un vehiculo por ID
    public Optional<Vehiculo> getVehiculoById(Long id) { 
        return vehiculoRepository.findById(id); 
    }

    // Guardar o actualizar un vehiculo
    public Vehiculo guardarVehiculo(Vehiculo vehiculo) {
        
        // 1. Lógica original: Si viene con cliente, validamos que exista en BD
        if (vehiculo.getCliente() != null && vehiculo.getCliente().getId() != null) {
            Cliente clienteReal = clienteRepository.findById(vehiculo.getCliente().getId()).orElse(null);
            if (clienteReal != null) {
                vehiculo.setCliente(clienteReal);
            }
        }

        // 2. ---> NUEVO: Buscamos y atamos el vehículo al Taller (Multi-Tenant)
        Taller miTaller = securityHelper.getTallerAutenticado();
            
        vehiculo.setTaller(miTaller); 

        // 3. Guardamos en la base de datos
        return vehiculoRepository.save(vehiculo);
    }

    // Eliminar un vehiculo por ID
    public void deleteVehiculo(Long id) { 
        vehiculoRepository.deleteById(id); 
    }
}