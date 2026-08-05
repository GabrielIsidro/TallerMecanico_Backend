package com.taller.backend.repository;

import com.taller.backend.model.PlanSuscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanSuscripcionRepository extends JpaRepository<PlanSuscripcion, Long> {
    List<PlanSuscripcion> findByActivoTrue();
}
