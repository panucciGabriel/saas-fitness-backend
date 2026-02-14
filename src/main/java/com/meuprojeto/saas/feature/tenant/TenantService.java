package com.meuprojeto.saas.feature.tenant;

import org.flywaydb.core.Flyway;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final DataSource dataSource;

    public TenantService(TenantRepository tenantRepository, DataSource dataSource) {
        this.tenantRepository = tenantRepository;
        this.dataSource = dataSource;
    }

    public void createTenant(String name, String email, String schemaName) {
        // 1. Salva o registro na tabela pública
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setOwnerEmail(email);
        tenant.setSchemaName(schemaName);
        tenant.setActive(true);
        tenantRepository.save(tenant);

        // 2. Cria o Schema no Banco de Dados (RAW JDBC)
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar schema: " + e.getMessage());
        }

        // 3. Roda o Flyway para criar as tabelas NESSE schema novo
        // ... dentro do método createTenant
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("db/tenants") // <--- ALTERE AQUI (removemos o "migration")
                .load();

        flyway.migrate();
    }
}