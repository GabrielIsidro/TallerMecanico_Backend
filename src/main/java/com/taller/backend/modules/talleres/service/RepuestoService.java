package com.taller.backend.modules.talleres.service;

import com.taller.backend.core.exception.ResourceNotFoundException;
import com.taller.backend.core.exception.UnauthorizedAccessException;
import com.taller.backend.core.security.SecurityHelper;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.talleres.dto.InventarioKpisDTO;
import com.taller.backend.modules.talleres.dto.MovimientoStockDTO;
import com.taller.backend.modules.talleres.model.MovimientoStock;
import com.taller.backend.modules.talleres.model.Repuesto;
import com.taller.backend.modules.talleres.model.TipoMovimiento;
import com.taller.backend.modules.talleres.repository.MovimientoStockRepository;
import com.taller.backend.modules.talleres.repository.RepuestoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RepuestoService {

    @Autowired
    private RepuestoRepository repuestoRepository;

    @Autowired
    private MovimientoStockRepository movimientoStockRepository;

    @Autowired
    private SecurityHelper securityHelper;

    /**
     * Obtiene repuestos paginados con filtros combinados: búsqueda por texto, categoría y estado de stock.
     */
    public Page<Repuesto> obtenerRepuestosPaginados(Pageable pageable, String search, String categoria, Boolean bajoStock, Boolean agotados) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        String searchTrim = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String categoriaTrim = (categoria != null && !categoria.trim().isEmpty()) ? categoria.trim() : null;
        boolean filterBajoStock = Boolean.TRUE.equals(bajoStock);
        boolean filterAgotados = Boolean.TRUE.equals(agotados);

        return repuestoRepository.findFiltrados(miTaller.getId(), searchTrim, categoriaTrim, filterBajoStock, filterAgotados, pageable);
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
     * Registra un repuesto asignándole el taller autenticado e inicia su movimiento inicial si tiene stock.
     */
    @Transactional
    public Repuesto guardarRepuesto(Repuesto repuesto) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        repuesto.setTaller(miTaller);
        repuesto.setTallerId(miTaller.getId());
        if (repuesto.getPrecioCosto() == null) {
            repuesto.setPrecioCosto(BigDecimal.ZERO);
        }
        Repuesto nuevo = repuestoRepository.save(repuesto);

        if (nuevo.getCantidad() != null && nuevo.getCantidad() > 0) {
            MovimientoStock mov = new MovimientoStock(
                    nuevo,
                    nuevo.getTallerId(),
                    TipoMovimiento.ENTRADA,
                    nuevo.getCantidad(),
                    0,
                    nuevo.getCantidad(),
                    "Stock inicial de alta",
                    getUsuarioActualNombre(),
                    null
            );
            movimientoStockRepository.save(mov);
        }

        return nuevo;
    }

    /**
     * Actualiza los datos de un repuesto validando pertenencia y registrando ajuste si cambió el stock físico.
     */
    @Transactional
    public Repuesto actualizarRepuesto(Long id, Repuesto detalles) {
        Repuesto existente = obtenerPorId(id);

        int stockPrevio = existente.getCantidad() != null ? existente.getCantidad() : 0;
        int nuevoStock = detalles.getCantidad() != null ? detalles.getCantidad() : 0;

        existente.setNombre(detalles.getNombre());
        existente.setSku(detalles.getSku());
        existente.setMarca(detalles.getMarca());
        existente.setCategoria(detalles.getCategoria());
        existente.setUbicacion(detalles.getUbicacion());
        existente.setPrecioCosto(detalles.getPrecioCosto() != null ? detalles.getPrecioCosto() : BigDecimal.ZERO);
        existente.setProveedor(detalles.getProveedor());
        existente.setStockMinimo(detalles.getStockMinimo());
        existente.setPrecio(detalles.getPrecio());
        existente.setCantidad(nuevoStock);

        Repuesto guardado = repuestoRepository.save(existente);

        if (stockPrevio != nuevoStock) {
            int diff = Math.abs(nuevoStock - stockPrevio);
            TipoMovimiento tipo = nuevoStock > stockPrevio ? TipoMovimiento.ENTRADA : TipoMovimiento.SALIDA;
            MovimientoStock mov = new MovimientoStock(
                    guardado,
                    guardado.getTallerId(),
                    tipo,
                    diff,
                    stockPrevio,
                    nuevoStock,
                    "Modificación manual en edición de producto",
                    getUsuarioActualNombre(),
                    null
            );
            movimientoStockRepository.save(mov);
        }

        return guardado;
    }

    /**
     * Ajusta el stock de forma atómica (Entrada rápida, salida o ajuste) y registra el movimiento de Kardex.
     */
    @Transactional
    public Repuesto ajustarStock(Long repuestoId, TipoMovimiento tipo, Integer cantidad, String motivo, Long ordenId) {
        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        Repuesto repuesto = obtenerPorId(repuestoId);
        int stockPrevio = repuesto.getCantidad() != null ? repuesto.getCantidad() : 0;
        int stockPosterior;

        switch (tipo) {
            case ENTRADA -> stockPosterior = stockPrevio + cantidad;
            case SALIDA -> {
                if (stockPrevio < cantidad) {
                    throw new IllegalArgumentException("Stock insuficiente para '" + repuesto.getNombre() + "'. Stock actual: " + stockPrevio + ", solicitado: " + cantidad);
                }
                stockPosterior = stockPrevio - cantidad;
            }
            case AJUSTE -> stockPosterior = cantidad;
            default -> throw new IllegalArgumentException("Tipo de movimiento desconocido: " + tipo);
        }

        repuesto.setCantidad(stockPosterior);
        Repuesto guardado = repuestoRepository.save(repuesto);

        MovimientoStock mov = new MovimientoStock(
                guardado,
                guardado.getTallerId(),
                tipo,
                cantidad,
                stockPrevio,
                stockPosterior,
                (motivo != null && !motivo.isBlank()) ? motivo : "Ajuste de inventario",
                getUsuarioActualNombre(),
                ordenId
        );
        movimientoStockRepository.save(mov);

        return guardado;
    }

    /**
     * Obtiene el historial de movimientos de un repuesto en particular.
     */
    public Page<MovimientoStockDTO> obtenerMovimientosRepuesto(Long repuestoId, Pageable pageable) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        Repuesto repuesto = obtenerPorId(repuestoId);
        return movimientoStockRepository.findByRepuestoIdAndTallerIdOrderByFechaDesc(repuesto.getId(), miTaller.getId(), pageable)
                .map(this::convertirAMovimientoDTO);
    }

    /**
     * Obtiene los movimientos recientes de todo el taller.
     */
    public Page<MovimientoStockDTO> obtenerMovimientosTaller(Pageable pageable) {
        Taller miTaller = securityHelper.getTallerAutenticado();
        return movimientoStockRepository.findByTallerIdOrderByFechaDesc(miTaller.getId(), pageable)
                .map(this::convertirAMovimientoDTO);
    }

    /**
     * Calcula los KPIs financieros y operativos del inventario del taller.
     */
    public InventarioKpisDTO obtenerKpisInventario() {
        Taller miTaller = securityHelper.getTallerAutenticado();
        List<Repuesto> repuestos = repuestoRepository.findByTallerId(miTaller.getId());

        int totalArticulos = repuestos.size();
        int totalUnidades = 0;
        BigDecimal valorInvertidoCosto = BigDecimal.ZERO;
        BigDecimal valorVentaPotencial = BigDecimal.ZERO;
        int articulosBajoStock = 0;
        int articulosAgotados = 0;

        for (Repuesto r : repuestos) {
            int cant = r.getCantidad() != null ? r.getCantidad() : 0;
            int min = r.getStockMinimo() != null ? r.getStockMinimo() : 0;
            BigDecimal precioVenta = r.getPrecio() != null ? r.getPrecio() : BigDecimal.ZERO;
            BigDecimal precioCosto = r.getPrecioCosto() != null ? r.getPrecioCosto() : BigDecimal.ZERO;

            totalUnidades += cant;
            valorInvertidoCosto = valorInvertidoCosto.add(precioCosto.multiply(BigDecimal.valueOf(cant)));
            valorVentaPotencial = valorVentaPotencial.add(precioVenta.multiply(BigDecimal.valueOf(cant)));

            if (cant == 0) {
                articulosAgotados++;
                articulosBajoStock++;
            } else if (cant <= min) {
                articulosBajoStock++;
            }
        }

        BigDecimal gananciaEstimada = valorVentaPotencial.subtract(valorInvertidoCosto);

        return InventarioKpisDTO.builder()
                .totalArticulos(totalArticulos)
                .totalUnidades(totalUnidades)
                .valorInvertidoCosto(valorInvertidoCosto)
                .valorVentaPotencial(valorVentaPotencial)
                .gananciaEstimada(gananciaEstimada)
                .articulosBajoStock(articulosBajoStock)
                .articulosAgotados(articulosAgotados)
                .build();
    }

    /**
     * Obtiene la lista de categorías existentes en el taller.
     */
    public List<String> obtenerCategorias() {
        Taller miTaller = securityHelper.getTallerAutenticado();
        return repuestoRepository.findDistinctCategoriasByTallerId(miTaller.getId());
    }

    /**
     * Elimina un repuesto validando pertenencia al taller autenticado.
     */
    @Transactional
    public void eliminarRepuesto(Long id) {
        Repuesto existente = obtenerPorId(id);
        repuestoRepository.delete(existente);
    }

    private MovimientoStockDTO convertirAMovimientoDTO(MovimientoStock mov) {
        return MovimientoStockDTO.builder()
                .id(mov.getId())
                .repuestoId(mov.getRepuesto() != null ? mov.getRepuesto().getId() : null)
                .repuestoNombre(mov.getRepuesto() != null ? mov.getRepuesto().getNombre() : "N/D")
                .repuestoSku(mov.getRepuesto() != null ? mov.getRepuesto().getSku() : null)
                .tipo(mov.getTipo())
                .cantidad(mov.getCantidad())
                .stockPrevio(mov.getStockPrevio())
                .stockPosterior(mov.getStockPosterior())
                .motivo(mov.getMotivo())
                .usuarioNombre(mov.getUsuarioNombre())
                .ordenId(mov.getOrdenId())
                .fecha(mov.getFecha())
                .build();
    }

    private String getUsuarioActualNombre() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "Sistema";
    }
}
