package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.talleres.model.OrdenTrabajo;
import com.taller.backend.modules.backoffice.model.Taller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {
    List<OrdenTrabajo> findByTallerId(Long tallerId);
    List<OrdenTrabajo> findByVehiculoId(Long vehiculoId); // Método para encontrar órdenes de trabajo por el ID del vehículo
    Page<OrdenTrabajo> findByTaller(Taller taller, Pageable pageable);
    List<OrdenTrabajo> findByTaller(Taller taller);
    void deleteByTallerId(Long tallerId);

}
