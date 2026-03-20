package com.taller.backend.model;

/**
 * Enum que representa los estados posibles de una orden de trabajo.
 * Cada estado puede tener diferentes acciones asociadas.
 * - PENDIENTE: La orden ha sido creada pero no se ha presupuestado.
 * - PRESUPUESTADO: Se ha generado un presupuesto para la orden, esperando aprobación.
 * - APROBADO: El presupuesto ha sido aprobado, se puede iniciar la reparación.
 * - EN_REPARACION: La orden está siendo reparada.
 * - FINALIZADO: La reparación ha sido completada, esperando entrega.
 * - ENTREGADO: La orden ha sido entregada al cliente.
 */
public enum EstadoOrden {
    PENDIENTE,
    PRESUPUESTADO,
    APROBADO,
    EN_REPARACION,
    FINALIZADO,
    ENTREGADO
}
