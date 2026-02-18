package com.meuprojeto.saas.config;

import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class TenantMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantMigrationRunner.class);

    private final TenantRepository tenantRepository;
    private final DataSource dataSource;

    public TenantMigrationRunner(TenantRepository tenantRepository, DataSource dataSource) {
        this.tenantRepository = tenantRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        log.info("Iniciando migração dos Tenants...");

        List<Tenant> tenants = tenantRepository.findAll();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();
            log.info("Atualizando schema: {}", schema);

            try {
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/tenants") // Corrigido: adicionado classpath:
                        .schemas(schema)
                        .load();

                flyway.migrate();
            } catch (Exception e) {
                log.error("Falha ao migrar schema {}: {}", schema, e.getMessage());
            }
        }

        log.info("Todos os tenants foram verificados!");
    }
}