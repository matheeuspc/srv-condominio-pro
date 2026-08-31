-- V2 — Fluxo "esqueci minha senha" (POST /auth/forgot-password + /auth/reset-password).
-- Colunas dedicadas em `usuarios`, separadas do token de convite (token_convite) para não
-- colidir com um convite ainda pendente.

ALTER TABLE usuarios ADD COLUMN token_reset_senha     VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN token_reset_expiracao TIMESTAMP;

ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_token_reset_senha UNIQUE (token_reset_senha);
