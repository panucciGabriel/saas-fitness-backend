package com.meuprojeto.saas.config.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

    @Override
    public String resolveCurrentTenantIdentifier() {
        return TenantContext.getTenant();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        // false: evita erros ao acessar tabelas do schema 'public' (Tenant, Invite,
        // StudentDirectory)
        // enquanto o TenantContext está setado para um schema de tenant
        return false;
    }
}
