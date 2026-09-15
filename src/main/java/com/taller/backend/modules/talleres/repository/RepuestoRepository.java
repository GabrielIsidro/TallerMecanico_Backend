package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.talleres.model.Repuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepuestoRepository extends JpaRepository<Repuesto, Long> {
    List<Repuesto> findByTallerId(Long tallerId);
    void deleteByTallerId(Long tallerId);
}
