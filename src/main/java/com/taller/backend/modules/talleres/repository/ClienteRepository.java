package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.talleres.model.Cliente;
import com.taller.backend.modules.backoffice.model.Taller;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Búsqueda genérica por el nuevo campo (sirve para personas o empresas)
    List<Cliente> findByNombreClienteContainingIgnoreCase(String nombreCliente);
    Page<Cliente> findByNombreClienteContainingIgnoreCase(String nombreCliente, Pageable pageable);
    
    // Búsquedas por taller (Multi-Tenant)
    List<Cliente> findByTallerId(Long tallerId);
    Page<Cliente> findByTaller(Taller taller, Pageable pageable);
    List<Cliente> findByTaller(Taller taller);
    
    // Opcional pero recomendado: Búsqueda combinada para que un taller 
    // solo pueda buscar entre SUS propios clientes
    Page<Cliente> findByNombreClienteContainingIgnoreCaseAndTaller(String nombreCliente, Taller taller, Pageable pageable);
    void deleteByTallerId(Long tallerId);
}
