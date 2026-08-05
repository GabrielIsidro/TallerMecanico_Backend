package com.taller.backend.repository;

import com.taller.backend.model.Taller;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TallerRepository extends JpaRepository<Taller, Long> {
    // Más adelante podemos agregar búsquedas por nombre o email acá
}