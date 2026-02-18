-- Migration de segurança: garante que as colunas phone e age existam
-- em schemas de tenants que foram criados antes dessas colunas serem adicionadas.
-- O uso de IF NOT EXISTS evita erros em bancos já atualizados.

ALTER TABLE students ADD COLUMN IF NOT EXISTS phone VARCHAR(255);
ALTER TABLE students ADD COLUMN IF NOT EXISTS age INTEGER;
