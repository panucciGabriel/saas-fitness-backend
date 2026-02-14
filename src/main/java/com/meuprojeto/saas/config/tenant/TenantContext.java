package com.meuprojeto.saas.config.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TenantContext {

    private static final Logger log = LoggerFactory.getLogger(TenantContext.class);
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    // Padrão caso nada seja informado
    private static final String DEFAULT_TENANT = "public";

    private TenantContext() {}

    // Define o Tenant da vez (Schema)
    public static void setTenant(String tenant) {
        log.debug("Setting tenant to: {}", tenant);
        currentTenant.set(tenant);
    }

    // Recupera o Tenant da vez
    public static String getTenant() {
        String tenant = currentTenant.get();
        return tenant != null ? tenant : DEFAULT_TENANT;
    }

    // Limpa a memória (Essencial!)
    public static void clear() {
        currentTenant.remove();
    }
}