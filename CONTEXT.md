# CondomínioPro — Contexto do Projeto

## 1. Visão Geral

SaaS de gestão de condomínios focado em **self-service**, com mínimo de suporte humano e self checkout.

### Problema que resolve
- Reservas de áreas comuns feitas de forma manual (caderno, WhatsApp, papel)
- Comunicação entre síndico e moradores dispersa e ineficiente
- Falta de centralização de informações (FAQs, regras, avisos)
- Síndico sobrecarregado com perguntas repetitivas

### Fora do escopo (MVP)
- Controle de acesso e visitantes
- Sistema financeiro completo (boletos, inadimplência)
- Abertura de chamados de manutenção
- Votações e assembleias
- App mobile nativo (futuro)
- Bot de WhatsApp interativo (futuro)

---

## 2. Escopo do MVP

### Módulos incluídos
- Cadastro de moradores (Proprietário / Inquilino)
- Gestão de unidades (bloco + número)
- Reserva de áreas comuns com pagamento via Pix
- Comunicados do síndico (feed de avisos)
- FAQ configurável por categoria
- Notificações por email e WhatsApp
- Dashboard básico para o síndico
- Sistema de convite para moradores (link de primeiro acesso)

### Plataforma
- Web app responsivo (mobile first)
- PWA — morador pode salvar na tela inicial do celular
- App nativo planejado para fase posterior

### Modelo de Negócio
Planos mensais por número de unidades:
- Até 50 unidades: R$ 99/mês
- 51–150 unidades: R$ 249/mês
- 151–300 unidades: R$ 449/mês
- Acima de 300: customizado

Add-ons:
- Notificações WhatsApp: R$ 0,15/mensagem
- Taxa sobre reservas pagas: 2,5% + taxa do gateway

---

## 3. Stack Técnica

### Backend
- **Java 25 + Spring Boot + Maven**
- Arquitetura: Monolito Modular
- Camadas: Controller → Service → Repository → Database
- Banco de dados: PostgreSQL (Supabase)
- ORM: Spring Data JPA / Hibernate
- Autenticação: JWT (Spring Security)
- Pagamentos: Mercado Pago (Pix)
- Email: Resend ou SendGrid
- WhatsApp: Twilio

### Frontend
- React + TypeScript (gerado via Lovable)
- Tailwind CSS + shadcn/ui
- react-router-dom para navegação
- Hospedagem: Vercel

### Infraestrutura
- Banco: Supabase (PostgreSQL gerenciado)
- Storage: Cloudflare R2 ou AWS S3
- Backend hosting: Railway ou Render

---

## 4. Perfis de Usuário (Roles)

| Role | Descrição |
|------|-----------|
| `SINDICO` | Administrador do condomínio. Acesso total. |
| `PROPRIETARIO` | Dono da unidade. Pode reservar áreas, ver comunicados, FAQ. |
| `INQUILINO` | Locatário. Mesmos acessos do proprietário, sem dados financeiros sensíveis. |

> **Decisão de produto:** no MVP um usuário pertence a apenas um condomínio. Suporte a múltiplos condomínios (administradoras) é planejado para versão futura.

---

## 5. Regras de Negócio

### 5.1 Moradores
- Síndico cadastra o morador e o sistema envia convite por email com link de primeiro acesso
- O morador define sua senha no primeiro acesso via token de convite (expira em 7 dias)
- Um morador pode estar vinculado a mais de uma unidade
- A exclusão de morador é lógica (soft delete): campo `ativo = false` e vínculo com unidade encerrado
- Não é possível deletar uma unidade que tenha moradores ativos

### 5.2 Reservas
Cada área comum possui regras próprias configuradas pelo síndico:
- Horário de funcionamento (início e fim)
- Antecedência mínima e máxima para reserva (em horas/dias)
- Limite mensal de reservas por morador (opcional)
- Taxa de uso (R$) — pode ser zero
- Requer aprovação do síndico (sim/não)

**Fluxo com taxa:**
Morador escolhe horário → Gera QR Code Pix → Pagamento confirmado via webhook → Reserva confirmada automaticamente

**Fluxo sem taxa:**
Morador escolhe horário → Reserva confirmada (ou pendente se requer aprovação)

**Status possíveis:** `PENDENTE`, `CONFIRMADA`, `CANCELADA`, `REJEITADA`

### 5.3 Comunicados
- Síndico pode segmentar por: Todos, Só Proprietários, Só Inquilinos
- Sistema registra quais moradores leram cada comunicado
- Comunicado pode ter anexo (PDF ou imagem)

### 5.4 FAQ
- Categorias: `RESERVAS`, `REGRAS`, `TAXAS`, `SERVICOS`, `OUTROS`
- Cada FAQ tem ordem de exibição configurável
- Moradores podem buscar FAQs por palavra-chave

### 5.5 Notificações
- Eventos: reserva confirmada, reserva cancelada, novo comunicado, convite de cadastro
- Canais: Email (obrigatório) e WhatsApp (opcional, configurável pelo condomínio)
- Morador pode configurar suas preferências de notificação

---

## 6. Endpoints da API

**Base URL:** `/api/v1`
**Autenticação:** `Authorization: Bearer {token}` (JWT)

### 6.1 Auth (público)
```
POST   /auth/register              → Criar conta (condomínio + síndico)
POST   /auth/login                 → Login
GET    /auth/me                    → Dados do usuário logado
POST   /auth/forgot-password       → Solicitar reset de senha
POST   /auth/reset-password        → Redefinir senha
POST   /auth/refresh-token         → Renovar token JWT
```

### 6.2 Condomínios (SINDICO)
```
GET    /condominios/:id            → Dados do condomínio
PUT    /condominios/:id            → Atualizar dados
GET    /condominios/:id/dashboard  → Métricas do dashboard
```

### 6.3 Unidades (SINDICO)
```
GET    /condominios/:condominioId/unidades      → Listar
POST   /condominios/:condominioId/unidades      → Criar
GET    /unidades/:id                            → Buscar
PUT    /unidades/:id                            → Atualizar
DELETE /unidades/:id                            → Deletar
```

### 6.4 Moradores
```
GET    /condominios/:condominioId/moradores     → Listar (SINDICO)
POST   /condominios/:condominioId/moradores     → Criar + enviar convite (SINDICO)
GET    /moradores/:id                           → Buscar (SINDICO)
PUT    /moradores/:id                           → Atualizar (SINDICO)
DELETE /moradores/:id                           → Desativar soft delete (SINDICO)
POST   /moradores/:id/vincular-unidade          → Vincular a unidade (SINDICO)
GET    /moradores/me                            → Dados do morador logado (MORADOR)
POST   /moradores/convite/:token                → Aceitar convite e definir senha (público)
```

### 6.5 Áreas Comuns
```
GET    /condominios/:condominioId/areas         → Listar (autenticado)
POST   /condominios/:condominioId/areas         → Criar (SINDICO)
GET    /areas/:id                               → Buscar (autenticado)
PUT    /areas/:id                               → Atualizar (SINDICO)
DELETE /areas/:id                               → Deletar (SINDICO)
GET    /areas/:id/disponibilidade?data=         → Horários disponíveis (autenticado)
```

### 6.6 Reservas
```
POST   /reservas                               → Criar reserva (MORADOR)
GET    /reservas/:id                           → Buscar (autenticado)
DELETE /reservas/:id                           → Cancelar (MORADOR)
GET    /moradores/me/reservas                  → Minhas reservas (MORADOR)
GET    /areas/:areaId/reservas                 → Reservas de uma área (SINDICO)
GET    /condominios/:condominioId/reservas     → Todas as reservas (SINDICO)
PUT    /reservas/:id/aprovar                   → Aprovar (SINDICO)
PUT    /reservas/:id/rejeitar                  → Rejeitar (SINDICO)
POST   /reservas/validar                       → Validar antes de criar (MORADOR)
```

### 6.7 Pagamentos
```
POST   /pagamentos/criar-cobranca              → Gerar QR Code Pix (MORADOR)
GET    /pagamentos/:id/status                  → Consultar status (autenticado)
POST   /pagamentos/webhook/mercadopago         → Webhook MP (público, validado por assinatura)
GET    /reservas/:reservaId/pagamento          → Pagamento de uma reserva (autenticado)
GET    /condominios/:condominioId/pagamentos   → Relatório financeiro (SINDICO)
```

### 6.8 Avisos
```
POST   /condominios/:condominioId/avisos       → Criar (SINDICO)
GET    /condominios/:condominioId/avisos       → Listar (autenticado)
GET    /avisos/:id                             → Buscar (autenticado)
PUT    /avisos/:id                             → Atualizar (SINDICO)
DELETE /avisos/:id                             → Deletar (SINDICO)
POST   /avisos/:id/marcar-lido                 → Marcar como lido (MORADOR)
GET    /avisos/nao-lidos                       → Avisos não lidos (MORADOR)
```

### 6.9 FAQs
```
GET    /condominios/:condominioId/faqs                    → Listar (autenticado)
POST   /condominios/:condominioId/faqs                    → Criar (SINDICO)
GET    /faqs/:id                                          → Buscar (autenticado)
PUT    /faqs/:id                                          → Atualizar (SINDICO)
DELETE /faqs/:id                                          → Deletar (SINDICO)
GET    /condominios/:condominioId/faqs/search?q=          → Buscar por keyword (autenticado)
```

### 6.10 Notificações
```
GET    /moradores/me/preferencias-notificacoes            → Ver preferências (MORADOR)
PUT    /moradores/me/preferencias-notificacoes            → Atualizar preferências (MORADOR)
GET    /condominios/:condominioId/notificacoes/log        → Log de envios (SINDICO)
POST   /notificacoes/enviar                               → Envio manual (SINDICO)
```

---

## 7. Schema do Banco de Dados

### Enums
```sql
CREATE TYPE "Role" AS ENUM ('SINDICO', 'PROPRIETARIO', 'INQUILINO');
CREATE TYPE "StatusReserva" AS ENUM ('PENDENTE', 'CONFIRMADA', 'CANCELADA', 'REJEITADA');
CREATE TYPE "StatusPagamento" AS ENUM ('AGUARDANDO', 'PAGO', 'FALHOU', 'ESTORNADO');
CREATE TYPE "StatusMorador" AS ENUM ('ATIVO', 'INATIVO');
CREATE TYPE "CategoriaFAQ" AS ENUM ('RESERVAS', 'REGRAS', 'TAXAS', 'SERVICOS', 'OUTROS');
CREATE TYPE "TipoNotificacao" AS ENUM ('EMAIL', 'WHATSAPP');
CREATE TYPE "StatusNotificacao" AS ENUM ('PENDENTE', 'ENVIADO', 'FALHOU');
```

### Tabelas
```sql
-- CONDOMÍNIOS
CREATE TABLE condominios (
    id                SERIAL PRIMARY KEY,
    nome              VARCHAR(255)  NOT NULL,
    cnpj              VARCHAR(20)   NOT NULL UNIQUE,
    endereco          VARCHAR(500)  NOT NULL,
    telefone          VARCHAR(20),
    logo_url          VARCHAR(500),
    plano             VARCHAR(50)   NOT NULL DEFAULT 'TRIAL',
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    notifica_email    BOOLEAN       NOT NULL DEFAULT TRUE,
    notifica_whatsapp BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- UNIDADES
CREATE TABLE unidades (
    id            SERIAL PRIMARY KEY,
    bloco         VARCHAR(10),
    numero        VARCHAR(10)  NOT NULL,
    condominio_id INTEGER      NOT NULL REFERENCES condominios(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (condominio_id, bloco, numero)
);

-- USUÁRIOS
CREATE TABLE usuarios (
    id               SERIAL PRIMARY KEY,
    nome             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    senha            VARCHAR(500) NOT NULL,
    telefone         VARCHAR(20),
    role             "Role"       NOT NULL,
    ativo            BOOLEAN      NOT NULL DEFAULT TRUE,
    token_convite    VARCHAR(255) UNIQUE,
    token_expiracao  TIMESTAMP,
    condominio_id    INTEGER      NOT NULL REFERENCES condominios(id),
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- MORADOR <-> UNIDADE
CREATE TABLE morador_unidades (
    id          SERIAL PRIMARY KEY,
    tipo        "Role"          NOT NULL,
    status      "StatusMorador" NOT NULL DEFAULT 'ATIVO',
    data_inicio TIMESTAMP       NOT NULL DEFAULT NOW(),
    data_fim    TIMESTAMP,
    usuario_id  INTEGER         NOT NULL REFERENCES usuarios(id),
    unidade_id  INTEGER         NOT NULL REFERENCES unidades(id),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    UNIQUE (usuario_id, unidade_id)
);

-- ÁREAS COMUNS
CREATE TABLE areas_comuns (
    id               SERIAL PRIMARY KEY,
    nome             VARCHAR(255)  NOT NULL,
    descricao        TEXT,
    capacidade       INTEGER,
    taxa             NUMERIC(10,2) NOT NULL DEFAULT 0,
    requer_aprovacao BOOLEAN       NOT NULL DEFAULT FALSE,
    ativa            BOOLEAN       NOT NULL DEFAULT TRUE,
    foto_url         VARCHAR(500),
    horario_inicio   VARCHAR(5)    NOT NULL,
    horario_fim      VARCHAR(5)    NOT NULL,
    antecedencia_min INTEGER       NOT NULL DEFAULT 1,
    antecedencia_max INTEGER       NOT NULL DEFAULT 30,
    limite_mensal    INTEGER,
    condominio_id    INTEGER       NOT NULL REFERENCES condominios(id),
    created_at       TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- RESERVAS
CREATE TABLE reservas (
    id          SERIAL PRIMARY KEY,
    data        TIMESTAMP       NOT NULL,
    hora_inicio VARCHAR(5)      NOT NULL,
    hora_fim    VARCHAR(5)      NOT NULL,
    status      "StatusReserva" NOT NULL DEFAULT 'PENDENTE',
    observacao  TEXT,
    area_id     INTEGER         NOT NULL REFERENCES areas_comuns(id),
    usuario_id  INTEGER         NOT NULL REFERENCES usuarios(id),
    unidade_id  INTEGER         NOT NULL REFERENCES unidades(id),
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- PAGAMENTOS
CREATE TABLE pagamentos (
    id                SERIAL PRIMARY KEY,
    valor             NUMERIC(10,2)     NOT NULL,
    status            "StatusPagamento" NOT NULL DEFAULT 'AGUARDANDO',
    metodo_pagamento  VARCHAR(20)       NOT NULL DEFAULT 'PIX',
    mp_payment_id     VARCHAR(255)      UNIQUE,
    mp_qr_code        TEXT,
    mp_qr_code_base64 TEXT,
    mp_ticket_url     VARCHAR(500),
    paid_at           TIMESTAMP,
    reserva_id        INTEGER           NOT NULL UNIQUE REFERENCES reservas(id),
    created_at        TIMESTAMP         NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP         NOT NULL DEFAULT NOW()
);

-- AVISOS
CREATE TABLE avisos (
    id            SERIAL PRIMARY KEY,
    titulo        VARCHAR(255) NOT NULL,
    conteudo      TEXT         NOT NULL,
    anexo_url     VARCHAR(500),
    publicado     BOOLEAN      NOT NULL DEFAULT TRUE,
    destinatario  "Role",
    condominio_id INTEGER      NOT NULL REFERENCES condominios(id),
    autor_id      INTEGER      NOT NULL REFERENCES usuarios(id),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- AVISO LEITURAS
CREATE TABLE aviso_leituras (
    id         SERIAL PRIMARY KEY,
    lido       BOOLEAN   NOT NULL DEFAULT TRUE,
    lido_em    TIMESTAMP NOT NULL DEFAULT NOW(),
    aviso_id   INTEGER   NOT NULL REFERENCES avisos(id),
    usuario_id INTEGER   NOT NULL,
    UNIQUE (aviso_id, usuario_id)
);

-- FAQS
CREATE TABLE faqs (
    id            SERIAL PRIMARY KEY,
    pergunta      TEXT           NOT NULL,
    resposta      TEXT           NOT NULL,
    categoria     "CategoriaFAQ" NOT NULL DEFAULT 'OUTROS',
    ordem         INTEGER        NOT NULL DEFAULT 0,
    ativa         BOOLEAN        NOT NULL DEFAULT TRUE,
    condominio_id INTEGER        NOT NULL REFERENCES condominios(id),
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- NOTIFICAÇÕES
CREATE TABLE notificacoes (
    id         SERIAL PRIMARY KEY,
    tipo       "TipoNotificacao"   NOT NULL,
    status     "StatusNotificacao" NOT NULL DEFAULT 'PENDENTE',
    assunto    VARCHAR(255),
    conteudo   TEXT                NOT NULL,
    enviado_em TIMESTAMP,
    erro       TEXT,
    usuario_id INTEGER             NOT NULL REFERENCES usuarios(id),
    created_at TIMESTAMP           NOT NULL DEFAULT NOW()
);
```

---

## 8. Estrutura de Pacotes (Java)

```
com.condominiopro/
├── config/
│   ├── SecurityConfig.java
│   └── JwtConfig.java
├── modules/
│   ├── auth/
│   │   ├── AuthController.java
│   │   ├── AuthService.java
│   │   └── dto/
│   ├── condominios/
│   │   ├── CondominiosController.java
│   │   ├── CondominiosService.java
│   │   ├── CondominioRepository.java
│   │   ├── Condominio.java (entity)
│   │   └── dto/
│   ├── unidades/
│   ├── moradores/
│   ├── areas/
│   ├── reservas/
│   ├── pagamentos/
│   ├── avisos/
│   ├── faqs/
│   └── notificacoes/
└── shared/
    ├── exceptions/
    │   ├── AppException.java
    │   ├── NotFoundException.java
    │   └── GlobalExceptionHandler.java
    ├── security/
    │   ├── JwtFilter.java
    │   └── JwtService.java
    └── utils/
```

---

## 9. Ordem de Implementação (Backend)

```
Fase 1 — Base
  1. Configurar Spring Security + JWT
  2. AuthController (register, login, me)
  3. CondominiosController
  4. UnidadesController
  5. MoradoresController

Fase 2 — Core do produto
  6. AreasController
  7. ReservasController
  8. PagamentosController (Mercado Pago + webhook)

Fase 3 — Comunicação
  9. AvisosController
  10. FAQsController
  11. NotificacoesController (email + WhatsApp)

Fase 4 — Extras
  12. RelatoriosController
```