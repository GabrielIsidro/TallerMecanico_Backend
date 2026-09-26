package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.talleres.model.Repuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {
    List<Repuesto> findByTallerId(Long tallerId);
    Page<Repuesto> findByTallerId(Long tallerId, Pageable pageable);
    Page<Repuesto> findByNombreContainingIgnoreCaseAndTallerId(String nombre, Long tallerId, Pageable pageable);

    @Query("SELECT r FROM Repuesto r WHERE r.tallerId = :tallerId AND r.cantidad <= r.stockMinimo")
    Page<Repuesto> findBajoStockByTallerId(@Param("tallerId") Long tallerId, Pageable pageable);

    @Query("SELECT r FROM Repuesto r WHERE r.tallerId = :tallerId AND LOWER(r.nombre) LIKE LOWER(CONCAT('%', :search, '%')) AND r.cantidad <= r.stockMinimo")
    Page<Repuesto> findBajoStockAndNombreByTallerId(@Param("tallerId") Long tallerId, @Param("search") String search, Pageable pageable);

    void deleteByTallerId(Long tallerId);
}
