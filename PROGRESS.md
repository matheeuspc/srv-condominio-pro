# Progresso do Backend — CondomínioPro

Acompanha a "Ordem de Implementação" definida em `CONTEXT.md` (seção 9). Atualizado em 2026-08-30.

## Fase 1 — Base ✅ concluída

| # | Item | Status |
|---|------|--------|
| 1 | Spring Security + JWT | ✅ |
| 2 | AuthController (register, login, me) | ✅ |
| 3 | CondominiosController | ✅ |
| 4 | UnidadesController | ✅ |
| 5 | MoradoresController | ✅ |

Todos os endpoints foram testados manualmente (via curl) contra uma instância real do Supabase, não só compilados.

### O que ficou fora do escopo da Fase 1 (deliberadamente)

- **Auth**: `forgot-password`, `reset-password`, `refresh-token` (listados na seção 6.1 do CONTEXT.md, mas não fazem parte da Fase 1 do roadmap).
- **Moradores**: envio de convite por **email** ainda não existe — depende do módulo de Notificações (Fase 3). Por enquanto o `tokenConvite` volta na resposta do `POST /moradores` para o síndico repassar manualmente.

## Fase 2 — Core do produto (em andamento)

| # | Item | Status |
|---|------|--------|
| 6 | AreasController | ✅ |
| 7 | ReservasController | ⬜ próximo |
| 8 | PagamentosController (Mercado Pago + webhook) | ⬜ |

### Limitações conhecidas do AreasController (dependem do Reservas, ainda não implementado)

- `DELETE /areas/:id` não bloqueia exclusão de área com reservas ativas.
- `GET /areas/:id/disponibilidade` gera os blocos de horário a partir do funcionamento da área, mas ainda não descarta horários já reservados.

## Fase 3 — Comunicação (não iniciada)

- AvisosController
- FAQsController
- NotificacoesController (email + WhatsApp)

## Fase 4 — Extras (não iniciada)

- RelatoriosController

---

## Notas de infraestrutura

- **Banco**: Postgres gerenciado pelo Supabase (não é um banco local/efêmero — é a instância real do projeto).
- **`schema.sql`** (`src/main/resources/schema.sql`, roda em todo boot via `spring.sql.init.mode: always`): script idempotente que alinha tabelas que já existiam no Supabase (de uma tentativa anterior, com colunas em camelCase) ao schema snake_case que as entidades JPA esperam. `ddl-auto=update` cuida de tabelas/colunas genuinamente novas.
- **Credenciais**: `application.yaml` só tem defaults seguros (placeholders locais); as credenciais reais do Supabase ficam em `src/main/resources/application-local.yaml`, que está no `.gitignore` — rodar com `--spring-boot.run.profiles=local` (ou `SPRING_PROFILES_ACTIVE=local`) localmente.
- **Sem suíte de testes automatizados ainda**: toda verificação até aqui foi manual (curl) contra o Supabase real, subindo a aplicação localmente a cada mudança. Vale considerar testes de integração antes de avançar muito mais.
- **Dado legado preservado**: existe um condomínio real "Residencial das Flores" (id=1) com 2 unidades, que já estava no banco antes deste trabalho começar — foi preservado ao corrigir o `schema.sql`, mas perdeu metadados de baixo valor (created_at/updated_at originais, preferências de notificação) durante uma correção de schema anterior a eu saber que era dado real.
- **Dados de teste**: condomínio "Condomínio Teste Auth ATUALIZADO" (id=3) e usuários/moradores/áreas/unidades associados foram criados durante os testes manuais e continuam no banco — ainda não foram limpos.

## Estrutura de pacotes atual

```
com.mcardoso.srvcondominiopro/
├── config/
│   └── SecurityConfig.java
├── modules/
│   ├── auth/            (AuthController, AuthService, dto/)
│   ├── condominios/      (Condominio, CondominioRepository, CondominioController, CondominioService, dto/)
│   ├── usuarios/          (Usuario, Role, UsuarioRepository)
│   ├── unidades/          (Unidade, UnidadeRepository, UnidadeController, UnidadeService, dto/)
│   ├── moradores/         (MoradorUnidade, StatusMorador, MoradorUnidadeRepository, MoradorController, MoradorService, dto/)
│   └── areas/             (AreaComum, AreaComumRepository, AreaComumController, AreaComumService, dto/)
└── shared/
    ├── exceptions/        (AppException, NotFoundException, ConflictException, ForbiddenException, GlobalExceptionHandler)
    └── security/          (JwtService, JwtFilter)
```
