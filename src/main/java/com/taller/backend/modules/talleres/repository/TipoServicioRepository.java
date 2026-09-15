package com.taller.backend.modules.talleres.repository;

import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.TipoServicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TipoServicioRepository extends JpaRepository<TipoServicio, Long> {

    List<TipoServicio> findByTaller(Taller taller);
    void deleteByTallerId(Long tallerId);
}
