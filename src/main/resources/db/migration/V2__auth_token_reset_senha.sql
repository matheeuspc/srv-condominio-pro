-- V2 — Fluxo "esqueci minha senha" (POST /auth/forgot-password + /auth/reset-password).
-- Colunas dedicadas em `usuarios`, separadas do token de convite (token_convite) para não
-- colidir com um convite ainda pendente.
--
-- Idempotente: numa instância que já subiu com ddl-auto=update essas colunas/constraint
-- podem já existir. `IF NOT EXISTS` cobre as colunas; a constraint é recriada num bloco DO.

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_reset_senha     VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS token_reset_expiracao TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_usuarios_token_reset_senha'
    ) THEN
        ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_token_reset_senha UNIQUE (token_reset_senha);
    END IF;
END $$;
