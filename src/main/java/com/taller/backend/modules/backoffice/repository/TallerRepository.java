package com.taller.backend.modules.backoffice.repository;

import com.taller.backend.modules.backoffice.model.Taller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TallerRepository extends JpaRepository<Taller, Long> {
    // Más adelante podemos agregar búsquedas por nombre o email acá
}
