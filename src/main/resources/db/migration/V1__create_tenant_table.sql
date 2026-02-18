-- Cria a tabela de Tenants no esquema PUBLIC
CREATE TABLE public.tenants (
                                id BIGSERIAL PRIMARY KEY,
                                name VARCHAR(255) NOT NULL UNIQUE,
                                schema_name VARCHAR(63) NOT NULL UNIQUE,
                                owner_email VARCHAR(255) NOT NULL UNIQUE,
                                password VARCHAR(255) NOT NULL,
                                active BOOLEAN DEFAULT TRUE,
                                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);
-- Nota: Schemas de tenant são criados dinamicamente via TenantService ao registrar um novo Personal.
-- Não criar schemas hardcoded aqui para evitar dados de teste em produção.
