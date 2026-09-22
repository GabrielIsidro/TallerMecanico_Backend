package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.TipoServicio;
import com.taller.backend.modules.talleres.repository.TipoServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TipoServicioService {

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    @Autowired
    private SecurityHelper securityHelper;

    /**
     * Lista servicios pertenecientes al taller autenticado, con filtro opcional por grupo.
     */
    public List<TipoServicio> listarServicios(String grupo) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        if (grupo != null && !grupo.trim().isEmpty()) {
            return tipoServicioRepository.findByTallerAndGrupo(miTaller, grupo.trim().toUpperCase());
        }
        return tipoServicioRepository.findByTaller(miTaller);
    }

    public List<TipoServicio> listarServicios() {
        return listarServicios(null);
    }

    /**
     * Obtiene un servicio por ID validando pertenencia al taller autenticado.
     */
    public TipoServicio obtenerPorId(Long id) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        TipoServicio servicio = tipoServicioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado con ID: " + id));

        if (servicio.getTaller() != null && !servicio.getTaller().getId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: el servicio no pertenece a su taller");
        }

        return servicio;
    }

    /**
     * Registra un nuevo servicio vinculándolo obligatoriamente al taller autenticado.
     */
    @Transactional
    public TipoServicio guardar(TipoServicio tipoServicio) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        tipoServicio.setTaller(miTaller);
        tipoServicio.setTallerId(miTaller.getId());
        return tipoServicioRepository.save(tipoServicio);
    }

    /**
     * Actualiza un servicio validando pertenencia al taller autenticado.
     */
    @Transactional
    public TipoServicio actualizar(Long id, TipoServicio detalles) {
        TipoServicio existente = obtenerPorId(id);

        existente.setGrupo(detalles.getGrupo());
        existente.setDescripcion(detalles.getDescripcion());
        existente.setPrecioSugerido(detalles.getPrecioSugerido());

        return tipoServicioRepository.save(existente);
    }

    /**
     * Elimina un servicio validando pertenencia al taller autenticado.
     */
    @Transactional
    public void eliminar(Long id) {
        TipoServicio existente = obtenerPorId(id);
        tipoServicioRepository.delete(existente);
    }
}
