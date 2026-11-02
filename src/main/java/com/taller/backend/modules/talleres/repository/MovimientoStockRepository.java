package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.talleres.model.MovimientoStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    Page<MovimientoStock> findByTallerIdOrderByFechaDesc(Long tallerId, Pageable pageable);

    Page<MovimientoStock> findByRepuestoIdAndTallerIdOrderByFechaDesc(Long repuestoId, Long tallerId, Pageable pageable);

    List<MovimientoStock> findTop50ByTallerIdOrderByFechaDesc(Long tallerId);

    void deleteByTallerId(Long tallerId);
}
