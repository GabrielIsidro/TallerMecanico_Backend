package com.taller.backend.service;

import com.taller.backend.model.*;
import com.taller.backend.repository.OrdenTrabajoRepository;
import com.taller.backend.repository.VehiculoRepository;
import com.taller.backend.repository.TipoServicioRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrdenTrabajoService {

    @Autowired
    private OrdenTrabajoRepository ordenRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Autowired
    private TipoServicioRepository tipoServicioRepository;

    //Metodo Principal: Crea una orden completa calculando precios automaticos
    @Transactional
    public OrdenTrabajo crearOrden(Long vehiculoId, String descripcion, List<ItemOrden> items) {
        
        // 1. Buscamos el vehiculo
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId).orElseThrow(() -> new RuntimeException("Vehiculo no encontrado"));

        // 2. Creamos la orden de trabajo
        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setVehiculo(vehiculo);
        orden.setDescripcion(descripcion);
        orden.setEstado(EstadoOrden.PENDIENTE);

        // 3. Procesamos cada item de la orden
        for (ItemOrden item : items) {

            //Si el item viene con un ID de servicio
            if(item.getTipoServicio() != null && item.getTipoServicio().getId() != null) {
                //Buscamos el precio real en la BD
                TipoServicio servicioDb = tipoServicioRepository.findById(item.getTipoServicio().getId()).orElseThrow(() -> new RuntimeException ("Servicio no encontrado"));
                
                item.setTipoServicio(servicioDb);

                //Le pasamos la categoria del auto al item para que elija el precio correcto
                item.calcularSubtotal(vehiculo.getCategoria());
            }

            //Agregamos el item a la orden
            orden.agregarItem(item);
        }

        // 4. Guardamos
        return ordenRepository.save(orden);
    }

    //Metodo para obtener todas las ordenes de trabajo
    public List<OrdenTrabajo> obtenerTodas() {
        return ordenRepository.findAll();
    }

    //Metodo para guardar una orden de trabajo
    public OrdenTrabajo guardarOrden(OrdenTrabajo orden) {
        //Valida que venga con un vehiculo 
        if (orden.getVehiculo() != null && orden.getVehiculo().getId() != null) {
            //Busca el vehiculo en la base de datos
            Vehiculo vehiculoReal = vehiculoRepository.findById(orden.getVehiculo().getId()).orElse(null);

            //Si existe el vehiculo, lo asigna a la orden de trabajo
            if (vehiculoReal != null) {
                orden.setVehiculo(vehiculoReal);
            }

        }
        return ordenRepository.save(orden);
    }

    public OrdenTrabajo actualizarPago(Long id, FormaPago formaPago) {
        OrdenTrabajo orden = ordenRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setFormaPago(formaPago);
        return ordenRepository.save(orden);
    }
    
    //Metodo para obtener las ordenes de trabajo por el id del vehiculo
    public List<OrdenTrabajo> obtenerPorVehiculo(Long vehiculoId) {
        return ordenRepository.findByVehiculoId(vehiculoId);
    }

    //Metodo para cambiar el estado de una orden
    public OrdenTrabajo actualizarEstado(Long id, EstadoOrden nuevoEstado) {
        OrdenTrabajo orden = ordenRepository.findById(id).orElseThrow(()->new RuntimeException("Orden no encontrada"));
        orden.setEstado(nuevoEstado);
        return ordenRepository.save(orden);
    }

    //Metodo para actualizar el presupuesto de una orden de trabajo
    public OrdenTrabajo actualizarCosto(Long id, Double nuevoCosto){
        OrdenTrabajo orden = ordenRepository.findById(id).orElseThrow(()->new RuntimeException("Orden no encontrada"));
        orden.setCostoTotal(nuevoCosto);
        return ordenRepository.save(orden);
    }
}
