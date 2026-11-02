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

    @Query("SELECT r FROM Repuesto r WHERE r.tallerId = :tallerId " +
           "AND (:search IS NULL OR :search = '' OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(r.sku, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(r.marca, '')) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:categoria IS NULL OR :categoria = '' OR LOWER(r.categoria) = LOWER(:categoria)) " +
           "AND (:soloBajoStock = false OR r.cantidad <= r.stockMinimo) " +
           "AND (:soloAgotados = false OR r.cantidad = 0)")
    Page<Repuesto> findFiltrados(
            @Param("tallerId") Long tallerId,
            @Param("search") String search,
            @Param("categoria") String categoria,
            @Param("soloBajoStock") boolean soloBajoStock,
            @Param("soloAgotados") boolean soloAgotados,
            Pageable pageable
    );

    @Query("SELECT DISTINCT r.categoria FROM Repuesto r WHERE r.tallerId = :tallerId AND r.categoria IS NOT NULL AND TRIM(r.categoria) != '' ORDER BY r.categoria ASC")
    List<String> findDistinctCategoriasByTallerId(@Param("tallerId") Long tallerId);

    void deleteByTallerId(Long tallerId);
}
