-- V3 — Remove colunas camelCase legadas das tabelas das Fases 2-4.
--
-- Contexto: reservas, pagamentos, avisos, aviso_leituras, faqs, notificacoes e
-- preferencias_notificacao foram criadas num boot antigo com Hibernate SEM a
-- naming strategy snake_case, deixando colunas "horaInicio", "createdAt",
-- "updatedAt", etc. marcadas NOT NULL. As entidades JPA hoje mapeiam as versões
-- snake_case; o INSERT do Hibernate só preenche essas, e as camelCase (NOT NULL,
-- sem default) quebram o INSERT com erro 23502.
--
-- O antigo schema.sql (aposentado) só limpou as tabelas da Fase 1. Este script
-- estende a limpeza para as tabelas restantes. Idempotente (DROP ... IF EXISTS).

DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN
        SELECT table_name, column_name
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name IN (
              'reservas', 'pagamentos', 'avisos', 'aviso_leituras',
              'faqs', 'notificacoes', 'preferencias_notificacao'
          )
          AND column_name ~ '[A-Z]'
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP COLUMN IF EXISTS %I', r.table_name, r.column_name);
        RAISE NOTICE 'V3: dropped %.%', r.table_name, r.column_name;
    END LOOP;
END $$;
