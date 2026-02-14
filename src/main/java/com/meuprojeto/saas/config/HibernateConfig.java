package com.meuprojeto.saas.config;

import com.meuprojeto.saas.config.tenant.SchemaMultiTenantConnectionProvider;
import com.meuprojeto.saas.config.tenant.TenantIdentifierResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class HibernateConfig {

    // Pegamos essa configuração do application.yml
    @Value("${spring.jpa.show-sql:false}")
    private boolean showSql;

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            DataSource dataSource,
            SchemaMultiTenantConnectionProvider multiTenantConnectionProvider,
            TenantIdentifierResolver tenantIdentifierResolver
    ) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);

        // Define onde estão suas entidades (Pastas feature, shared, etc)
        em.setPackagesToScan("com.meuprojeto.saas");

        em.setJpaVendorAdapter(jpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();

        properties.put("hibernate.hbm2ddl.auto", "none");

        // 1. Configurações Básicas do Hibernate (Hardcoded para PostgreSQL)
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", showSql);
        properties.put("hibernate.format_sql", true);

        properties.put("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");

        // 2. Configurações de Multi-tenancy (O Coração do SaaS)
        // Usamos as Strings literais para evitar erro de importação da classe Environment
        properties.put("hibernate.multiTenancy", "SCHEMA");
        properties.put("hibernate.multi_tenant_connection_provider", multiTenantConnectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);

        em.setJpaPropertyMap(properties);
        return em;
    }
}