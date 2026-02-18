package com.meuprojeto.saas.config.tenant;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    private static final Logger log = LoggerFactory.getLogger(SchemaMultiTenantConnectionProvider.class);

    // Proteção contra SQL Injection: apenas letras, números e underscore são
    // permitidos
    private static final Pattern SAFE_SCHEMA_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");

    private final DataSource dataSource;

    public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private void validateSchema(String schema) {
        if (schema == null || !SAFE_SCHEMA_PATTERN.matcher(schema).matches()) {
            throw new HibernateException("Invalid tenant schema name: [" + schema
                    + "]. Only alphanumeric characters and underscores are allowed.");
        }
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        // Devolve ao pool sem fechar fisicamente
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        validateSchema(tenantIdentifier);
        Connection connection = getAnyConnection();
        try {
            connection.createStatement().execute("SET search_path TO " + tenantIdentifier);
            log.debug("Switched connection to schema: {}", tenantIdentifier);
        } catch (SQLException e) {
            throw new HibernateException(
                    "Could not alter JDBC connection to specified schema [" + tenantIdentifier + "]", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reseta para o schema padrão antes de devolver ao pool
            connection.createStatement().execute("SET search_path TO public");
            log.debug("Reset connection schema to public after tenant: {}", tenantIdentifier);
        } catch (SQLException e) {
            throw new HibernateException("Could not reset JDBC connection schema to [public]", e);
        }
        // Devolve ao pool (não fecha fisicamente — o pool gerencia o ciclo de vida)
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        return null;
    }
}
