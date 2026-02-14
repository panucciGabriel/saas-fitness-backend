package com.meuprojeto.saas.config;

import com.meuprojeto.saas.feature.tenant.Tenant;
import com.meuprojeto.saas.feature.tenant.TenantRepository;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class TenantMigrationRunner implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final DataSource dataSource;

    public TenantMigrationRunner(TenantRepository tenantRepository, DataSource dataSource) {
        this.tenantRepository = tenantRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        System.out.println("🚀 Iniciando migração dos Tenants...");

        List<Tenant> tenants = tenantRepository.findAll();

        for (Tenant tenant : tenants) {
            String schema = tenant.getSchemaName();
            System.out.println("⚡ Atualizando schema: " + schema);

            try {
                Flyway flyway = Flyway.configure()
                        .dataSource(dataSource)
                        .locations("db/tenants") // Pasta onde estão os SQLs V1, V2...
                        .schemas(schema) // Define o schema alvo
                        .load();

                flyway.migrate();
            } catch (Exception e) {
                System.err.println("❌ Falha ao migrar " + schema + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Todos os tenants foram verificados!");
    }
}