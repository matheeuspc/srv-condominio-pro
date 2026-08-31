-- Patch idempotente para alinhar tabelas já existentes no Supabase ao schema do CONTEXT.md.
-- Usa ADD COLUMN ... DEFAULT ... para o Postgres fazer backfill automático em linhas existentes,
-- evitando o erro "column contains null values" que o Hibernate (ddl-auto=update) não resolve sozinho.

ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS plano VARCHAR(50) NOT NULL DEFAULT 'TRIAL';
ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS notifica_email BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS notifica_whatsapp BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS condominios ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);
ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS token_convite VARCHAR(255);
ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS token_expiracao TIMESTAMP;
ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS condominio_id BIGINT;
ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS usuarios ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- unidades: colunas snake_case criadas com backfill a partir das colunas camelCase
-- legadas (preserva dados reais, ex.: condomínio "Residencial das Flores").
ALTER TABLE IF EXISTS unidades ADD COLUMN IF NOT EXISTS condominio_id BIGINT;
ALTER TABLE IF EXISTS unidades ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE IF EXISTS unidades ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Backfill de condominio_id/created_at/updated_at a partir das colunas camelCase já rodou
-- com sucesso num boot anterior (colunas camelCase já foram dropadas abaixo, então essas
-- linhas UPDATE não são mais necessárias nem podem rodar de novo — Spring executa este
-- script em todo boot via spring.sql.init.mode=always, e um DO $$ ... $$ com ';' internos
-- não é seguro aqui: o splitter de script do Spring é ingênuo e quebraria o bloco no meio).

ALTER TABLE IF EXISTS unidades ALTER COLUMN condominio_id SET NOT NULL;
ALTER TABLE IF EXISTS unidades ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE IF EXISTS unidades ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE IF EXISTS unidades ALTER COLUMN updated_at SET NOT NULL;
ALTER TABLE IF EXISTS unidades ALTER COLUMN updated_at SET DEFAULT NOW();

-- Colunas camelCase remanescentes de uma tentativa anterior (Hibernate sem naming strategy
-- snake_case), duplicadas das colunas acima. Confirmado com o usuário que são descartáveis.
ALTER TABLE IF EXISTS condominios DROP COLUMN IF EXISTS "logoUrl";
ALTER TABLE IF EXISTS condominios DROP COLUMN IF EXISTS "notificaEmail";
ALTER TABLE IF EXISTS condominios DROP COLUMN IF EXISTS "notificaWhatsapp";
ALTER TABLE IF EXISTS condominios DROP COLUMN IF EXISTS "createdAt";
ALTER TABLE IF EXISTS condominios DROP COLUMN IF EXISTS "updatedAt";

ALTER TABLE IF EXISTS usuarios DROP COLUMN IF EXISTS "tokenConvite";
ALTER TABLE IF EXISTS usuarios DROP COLUMN IF EXISTS "tokenExpiracao";
ALTER TABLE IF EXISTS usuarios DROP COLUMN IF EXISTS "createdAt";
ALTER TABLE IF EXISTS usuarios DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE IF EXISTS usuarios DROP COLUMN IF EXISTS "condominioId";

ALTER TABLE IF EXISTS unidades DROP COLUMN IF EXISTS "createdAt";
ALTER TABLE IF EXISTS unidades DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE IF EXISTS unidades DROP COLUMN IF EXISTS "condominioId";

-- morador_unidades: colunas camelCase legadas (tipo/status eram ENUM nativo do Postgres)
-- já foram dropadas num boot anterior. NÃO depender do Hibernate para recriá-las: ele gera
-- "ADD COLUMN ... NOT NULL" sem DEFAULT, que falha assim que a tabela tem alguma linha
-- (e passou a ter, pois os testes do MoradoresController já gravaram vínculos reais).
ALTER TABLE IF EXISTS morador_unidades DROP COLUMN IF EXISTS "dataInicio";
ALTER TABLE IF EXISTS morador_unidades DROP COLUMN IF EXISTS "dataFim";
ALTER TABLE IF EXISTS morador_unidades DROP COLUMN IF EXISTS "createdAt";
ALTER TABLE IF EXISTS morador_unidades DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE IF EXISTS morador_unidades DROP COLUMN IF EXISTS "usuarioId";
ALTER TABLE IF EXISTS morador_unidades DROP COLUMN IF EXISTS "unidadeId";

ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) NOT NULL DEFAULT 'PROPRIETARIO';
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ATIVO';
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS data_inicio TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS data_fim TIMESTAMP;
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS usuario_id BIGINT;
ALTER TABLE IF EXISTS morador_unidades ADD COLUMN IF NOT EXISTS unidade_id BIGINT;

-- areas_comuns: mesmo raciocínio acima — "taxa" (era double precision) foi dropada e
-- precisa ser recriada com DEFAULT explícito, já que a tabela já tem linha(s) reais.
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "requerAprovacao";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "fotoUrl";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "horarioInicio";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "horarioFim";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "antecedenciaMin";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "antecedenciaMax";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "limiteMensal";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "createdAt";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "updatedAt";
ALTER TABLE IF EXISTS areas_comuns DROP COLUMN IF EXISTS "condominioId";

ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS taxa NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS requer_aprovacao BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS foto_url VARCHAR(500);
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS horario_inicio VARCHAR(5) NOT NULL DEFAULT '08:00';
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS horario_fim VARCHAR(5) NOT NULL DEFAULT '22:00';
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS antecedencia_min INTEGER NOT NULL DEFAULT 1;
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS antecedencia_max INTEGER NOT NULL DEFAULT 30;
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS limite_mensal INTEGER;
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE IF EXISTS areas_comuns ADD COLUMN IF NOT EXISTS condominio_id BIGINT;
