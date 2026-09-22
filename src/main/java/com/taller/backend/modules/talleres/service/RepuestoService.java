package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.model.Repuesto;
import com.taller.backend.modules.talleres.repository.RepuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
public class RepuestoService {

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private SecurityHelper securityHelper;

    /**
     * Obtiene repuestos paginados con filtro opcional de búsqueda por nombre y/o bajo stock.
     */
    public Page<Repuesto> obtenerRepuestosPaginados(Pageable pageable, String search, Boolean soloBajoStock) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean filterBajoStock = Boolean.TRUE.equals(soloBajoStock);

        if (filterBajoStock) {
            if (hasSearch) {
                return repuestoRepository.findBajoStockAndNombreByTallerId(miTaller.getId(), search.trim(), pageable);
            }
            return repuestoRepository.findBajoStockByTallerId(miTaller.getId(), pageable);
        }

        if (hasSearch) {
            return repuestoRepository.findByNombreContainingIgnoreCaseAndTallerId(search.trim(), miTaller.getId(), pageable);
        }

        return repuestoRepository.findByTallerId(miTaller.getId(), pageable);
    }

    /**
     * Obtiene todos los repuestos del taller autenticado.
     */
    public List<Repuesto> listarRepuestos() {
        Taller miTaller = securityHelper.getTallerAutenticado();
        return repuestoRepository.findByTallerId(miTaller.getId());
    }

    /**
     * Obtiene un repuesto por ID verificando que pertenezca al taller autenticado.
     */
    public Repuesto obtenerPorId(Long id) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        Repuesto repuesto = repuestoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repuesto no encontrado con id: " + id));

        if (repuesto.getTallerId() != null && !repuesto.getTallerId().equals(miTaller.getId())) {
            throw new UnauthorizedAccessException("Acceso denegado: el repuesto no pertenece a su taller");
        }

        return repuesto;
    }

    /**
     * Registra un repuesto asignándole el taller autenticado.
     */
    @Transactional
    public Repuesto guardarRepuesto(Repuesto repuesto) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        repuesto.setTaller(miTaller);
        repuesto.setTallerId(miTaller.getId());
        return repuestoRepository.save(repuesto);
    }

    /**
     * Actualiza los datos de un repuesto validando pertenencia al taller autenticado.
     */
    @Transactional
    public Repuesto actualizarRepuesto(Long id, Repuesto detalles) {
        Repuesto existente = obtenerPorId(id);

        existente.setNombre(detalles.getNombre());
        existente.setSku(detalles.getSku());
        existente.setCantidad(detalles.getCantidad());
        existente.setStockMinimo(detalles.getStockMinimo());
        existente.setPrecio(detalles.getPrecio());

        return repuestoRepository.save(existente);
    }

    /**
     * Elimina un repuesto validando pertenencia al taller autenticado.
     */
    @Transactional
    public void eliminarRepuesto(Long id) {
        Repuesto existente = obtenerPorId(id);
        repuestoRepository.delete(existente);
    }
}
