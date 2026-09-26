package com.taller.backend.modules.backoffice.service;

import com.taller.backend.modules.backoffice.model.EstadoSuscripcion;
import com.taller.backend.modules.backoffice.model.Taller;
import com.taller.backend.modules.backoffice.repository.TallerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SuscripcionCronJob {

    @Autowired
    private TallerRepository tallerRepository;

    /**
     * Tarea programada que se ejecuta todos los días a las 00:00.
     * Busca los talleres cuya fecha de vencimiento haya pasado.
     * Si pasó entre 0 y 3 días, se marca como VENCIDA (días de gracia).
     * Si pasaron más de 3 días, se marca como SUSPENDIDA.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Ejecuta diariamente a medianoche
    @Transactional
    public void revisarSuscripcionesDiariamente() {
        System.out.println("--- Ejecutando Cron Job de Suscripciones: " + LocalDate.now() + " ---");

        List<Taller> todosLosTalleres = tallerRepository.findAll();
        LocalDate hoy = LocalDate.now();

        for (Taller taller : todosLosTalleres) {
            // Ignorar los que no tienen fecha de vencimiento (ej. Admin interno o si no aplica)
            if (taller.getFechaVencimiento() == null) continue;

            long diasDiferencia = ChronoUnit.DAYS.between(taller.getFechaVencimiento(), hoy);

            if (diasDiferencia > 3 && taller.getEstadoSuscripcion() != EstadoSuscripcion.SUSPENDIDA) {
                // Pasó el periodo de gracia
                taller.setEstadoSuscripcion(EstadoSuscripcion.SUSPENDIDA);
                tallerRepository.save(taller);
                System.out.println("Taller suspendido por falta de pago (+" + diasDiferencia + " días): " + taller.getNombre());
            } 
            else if (diasDiferencia > 0 && diasDiferencia <= 3 && taller.getEstadoSuscripcion() != EstadoSuscripcion.VENCIDA) {
                // Está en periodo de gracia
                taller.setEstadoSuscripcion(EstadoSuscripcion.VENCIDA);
                tallerRepository.save(taller);
                System.out.println("Taller marcado como vencido (en gracia): " + taller.getNombre());
            }
        }
    }
}
