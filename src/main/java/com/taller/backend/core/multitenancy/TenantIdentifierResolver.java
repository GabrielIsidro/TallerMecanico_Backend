package com.taller.backend.core.multitenancy;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Long> {

    @Override
    public Long resolveCurrentTenantIdentifier() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            return tenantId;
        }
        // Retorno por defecto o para cuando el backoffice lee todo
        return -1L; 
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
