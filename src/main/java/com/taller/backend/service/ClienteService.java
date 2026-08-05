package com.taller.backend.service;

import com.taller.backend.model.Taller;
import com.taller.backend.repository.TallerRepository;
import com.taller.backend.model.Cliente;
import com.taller.backend.repository.ClienteRepository;
import com.taller.backend.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
// import java.util.Optional;

/* 
Servicio para gestionar las operaciones relacionadas con los clientes
 */
@Service
public class ClienteService {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TallerRepository tallerRepository;

    @Autowired
    private SecurityHelper securityHelper;

    /*
    Método para obtener todos los clientes registrados en el sistema
     */
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findByTallerId(securityHelper.getTallerAutenticado().getId());
    }

    /*
    Método para guardar un nuevo cliente
     */
    public Cliente guardarCliente(Cliente cliente) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        cliente.setTaller(miTaller);
        return clienteRepository.save(cliente);
    }
}