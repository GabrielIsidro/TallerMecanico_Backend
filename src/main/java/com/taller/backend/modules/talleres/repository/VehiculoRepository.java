package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Vehiculo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    Optional<Vehiculo> findByPatente(String patente);
    List<Vehiculo> findByTallerId(Long tallerId);
    List<Vehiculo> findByTaller(Taller taller);
    Page<Vehiculo> findByTaller(Taller taller, Pageable pageable);
    Page<Vehiculo> findByPatenteContainingIgnoreCaseAndTaller(String patente, Taller taller, Pageable pageable);
    void deleteByTallerId(Long tallerId);
}
