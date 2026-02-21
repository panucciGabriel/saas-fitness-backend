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

    // 🌟 NOVO: Adicionado o parâmetro 'phone'
    public void createTenant(String name, String email, String schemaName, String hashedPassword, String phone) {
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setOwnerEmail(email);
        tenant.setSchemaName(schemaName);
        tenant.setPassword(hashedPassword);
        tenant.setPhone(phone); // 🌟 Salva o telefone aqui
        tenant.setActive(true);
        tenantRepository.save(tenant);

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar schema: " + e.getMessage());
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/tenants")
                .load();

        flyway.migrate();
    }
}