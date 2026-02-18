-- Adiciona coluna de senha (hash BCrypt) à tabela de tenants
-- Necessário para suportar autenticação com senha do Personal
ALTER TABLE public.tenants ADD COLUMN IF NOT EXISTS password VARCHAR(255);

-- Nota: Tenants existentes (tenant_a, tenant_b) precisarão ter a senha definida
-- via endpoint /auth/register ou atualização manual no banco.
