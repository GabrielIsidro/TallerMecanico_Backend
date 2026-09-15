package com.taller.backend.modules.talleres.controller;

import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.talleres.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/v1/talleres/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @GetMapping
    public ResponseEntity<Page<Cliente>> obtenerClientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Cliente> clientesPage;

        if (search != null && !search.trim().isEmpty()) {
            clientesPage = clienteRepository.findByNombreClienteContainingIgnoreCase(search.trim(), pageable);
        } else {
            clientesPage = clienteRepository.findAll(pageable);
        }

        return ResponseEntity.ok(clientesPage);
    }

    @PostMapping
    public Cliente crearCliente(@RequestBody Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, @RequestBody Cliente detallesCliente) {
        return clienteRepository.findById(id).map(cliente -> {
            cliente.setNombreCliente(detallesCliente.getNombreCliente());
            cliente.setTelefono(detallesCliente.getTelefono());
            cliente.setDireccion(detallesCliente.getDireccion());
            cliente.setEmail(detallesCliente.getEmail());
            cliente.setEsEmpresa(detallesCliente.getEsEmpresa());
            cliente.setDocumentoCuit(detallesCliente.getDocumentoCuit());

            Cliente actualizado = clienteRepository.save(cliente);
            return ResponseEntity.ok(actualizado);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        return clienteRepository.findById(id).map(cliente -> {
            clienteRepository.delete(cliente);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
