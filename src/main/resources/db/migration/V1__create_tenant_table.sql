-- Cria a tabela de Tenants no esquema PUBLIC
CREATE TABLE public.tenants (
                                id BIGSERIAL PRIMARY KEY,
                                name VARCHAR(255) NOT NULL UNIQUE,
                                schema_name VARCHAR(63) NOT NULL UNIQUE,
                                owner_email VARCHAR(255) NOT NULL,
                                active BOOLEAN DEFAULT TRUE,
                                created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Insere dois inquilinos de exemplo para testarmos depois
INSERT INTO public.tenants (name, schema_name, owner_email)
VALUES ('Academia Exemplo A', 'tenant_a', 'contato@academia-a.com');

INSERT INTO public.tenants (name, schema_name, owner_email)
VALUES ('Personal Trainer B', 'tenant_b', 'trainer@b.com');

-- ATENÇÃO: O Flyway vai rodar isso, mas NÃO vai criar os schemas (tenant_a, tenant_b) ainda.
-- Faremos isso dinamicamente depois ou manualmente agora para teste.
-- Por enquanto, vamos criar manualmente para ver a mágica:
CREATE SCHEMA IF NOT EXISTS tenant_a;
CREATE SCHEMA IF NOT EXISTS tenant_b;