package com.taller.backend.controller;

import com.taller.backend.model.Cliente;
import com.taller.backend.service.ClienteService;
import com.taller.backend.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/* 
Controlador para gestionar las operaciones relacionadas con los clientes
 */
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {
    
    @Autowired
    private ClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    /*
    Endpoint para listar todos los clientes
     */
    @GetMapping
    public List<Cliente> listarClientes() {
        return clienteService.obtenerTodos();
    }

    /*
    Endpoint para crear un nuevo cliente
     */
    @PostMapping
    public Cliente crearCliente(@RequestBody Cliente cliente) {
        return clienteService.guardarCliente(cliente);
    }

    /*
    Endpoint para actualizar un cliente existente
     */
    @PutMapping("/{id}")
    public Cliente actualizar(@PathVariable Long id, @RequestBody Cliente detalles) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado")); 

        cliente.setNombre(detalles.getNombre());
        cliente.setApellido(detalles.getApellido());
        cliente.setTelefono(detalles.getTelefono());
        cliente.setEmail(detalles.getEmail());

        return clienteRepository.save(cliente);
    }

    /*
    Endpoint para borrar un cliente
     */
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        clienteRepository.deleteById(id);
    }
}
