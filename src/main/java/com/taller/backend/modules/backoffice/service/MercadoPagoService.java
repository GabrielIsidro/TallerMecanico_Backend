package com.taller.backend.modules.backoffice.service;

import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.model.PlanSuscripcion;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MercadoPagoService {

    /**
     * Simula la creación de una Preferencia de Pago en MercadoPago.
     * En producción, aquí se usaría el SDK de MP con el ACCESS_TOKEN.
     */
    public String crearPreferenciaPago(Taller taller, PlanSuscripcion plan) {
        
        // 1. Aquí se validaría el monto según el plan
        double monto = plan.getPrecio();
        
        // 2. Aquí llamaríamos a MercadoPago API para obtener el preferenceId
        String fakePreferenceId = UUID.randomUUID().toString();
        
        // 3. Devolvemos una URL de checkout.
        // Como estamos en fase de prueba (sin token real), devolvemos un link simulado
        // que redirigirá de vuelta al frontend con un flag de éxito simulado.
        // En Producción devolveremos: "https://www.mercadopago.com.ar/checkout/v1/redirect?pref_id=" + preferenceId
        
        return "http://localhost:5173/?mp_simulation=success&pref_id=" + fakePreferenceId + "&planId=" + plan.getId();
    }
}
