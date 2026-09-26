package com.taller.backend.modules.talleres.model;
import com.taller.backend.modules.backoffice.model.Taller;

public enum RolUsuario {
    SUPER_ADMIN,    // Vos: Acceso total al panel de SaaS para gestionar talleres.
    ADMIN_TALLER,   // Néstor y otros dueños: Acceso a su propio taller, facturación y clientes.
    MECANICO        // Empleado: Solo ve autos y órdenes, pero no ve plata ni configuración.
}
