package com.taller.backend.controller;

import com.taller.backend.model.Cliente;
import com.taller.backend.model.Taller;
import com.taller.backend.model.Usuario;
import com.taller.backend.repository.ClienteRepository;
import com.taller.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    // ---> 1. Traemos la base de datos de usuarios
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ---> 2. Función mágica que descubre el Taller de la persona logueada
    private Taller getTallerAutenticado() {
        // Leemos el email que el patovica dejó en la memoria
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        // Buscamos al usuario en la base de datos
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        // Devolvemos su taller
        return usuario.getTaller();
    }

    @GetMapping
    public ResponseEntity<Page<Cliente>> obtenerClientes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search) {
        
        Taller miTaller = getTallerAutenticado();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        
        Page<Cliente> clientesPage;
        if (search != null && !search.trim().isEmpty()) {
            clientesPage = clienteRepository.findByNombreClienteContainingIgnoreCaseAndTaller(search.trim(), miTaller, pageable);
        } else {
            clientesPage = clienteRepository.findByTaller(miTaller, pageable);
        }
        
        return ResponseEntity.ok(clientesPage);
    }

    @PostMapping
    public Cliente crearCliente(@RequestBody Cliente cliente) {
        // ---> 4. Chau 1L. Le estampamos el taller verdadero al nuevo cliente
        cliente.setTaller(getTallerAutenticado());
        return clienteRepository.save(cliente);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizarCliente(@PathVariable Long id, @RequestBody Cliente detallesCliente) {
        return clienteRepository.findById(id).map(cliente -> {
            if (!cliente.getTaller().getId().equals(getTallerAutenticado().getId())) {
                throw new RuntimeException("Acceso denegado: El recurso no pertenece a tu taller.");
            }

            cliente.setNombreCliente(detallesCliente.getNombreCliente());
            cliente.setTelefono(detallesCliente.getTelefono());
            cliente.setDireccion(detallesCliente.getDireccion());
            cliente.setEmail(detallesCliente.getEmail());
            cliente.setEsEmpresa(detallesCliente.getEsEmpresa());
            cliente.setDocumentoCuit(detallesCliente.getDocumentoCuit());
            // No tocamos el Taller, ya tiene el correcto
            Cliente actualizado = clienteRepository.save(cliente);
            return ResponseEntity.ok(actualizado);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCliente(@PathVariable Long id) {
        return clienteRepository.findById(id).map(cliente -> {
            if (!cliente.getTaller().getId().equals(getTallerAutenticado().getId())) {
                throw new RuntimeException("Acceso denegado: El recurso no pertenece a tu taller.");
            }
            clienteRepository.delete(cliente);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
}