package com.taller.backend.service;

import com.taller.backend.model.Cliente;
import com.taller.backend.repository.ClienteRepository;
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

    /*
    Método para obtener todos los clientes registrados en el sistema
     */
    public List<Cliente> obtenerTodos() {
        return clienteRepository.findAll();
    }

    /*
    Método para guardar un nuevo cliente
     */
    public Cliente guardarCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }
}