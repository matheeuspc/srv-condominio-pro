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
| 7 | ReservasController | ✅ |
| 8 | PagamentosController (Mercado Pago + webhook) | ⬜ próximo |

### AreasController — limitações resolvidas pelo módulo de Reservas

- `DELETE /areas/:id` agora bloqueia (409) exclusão de área com reserva PENDENTE ou CONFIRMADA.
- `GET /areas/:id/disponibilidade` agora descarta os blocos de 1h que colidem com reservas PENDENTE/CONFIRMADA da data.

### ReservasController — regras implementadas (CONTEXT 5.2)

- Área precisa existir, estar `ativa` e pertencer ao condomínio do morador; a unidade precisa pertencer ao mesmo condomínio e o morador logado precisa ter vínculo **ATIVO** com ela.
- Horário validado contra o funcionamento da área e `horaInicio < horaFim`.
- **Antecedência**: `antecedenciaMin`/`antecedenciaMax` da área são interpretados em **dias** — `data` deve cair entre `hoje + min` e `hoje + max` (o CONTEXT diz "horas/dias"; escolhi dias por causa dos defaults 1 e 30).
- **Conflito**: nenhuma outra reserva PENDENTE/CONFIRMADA na mesma área/data com sobreposição de intervalo (bordas que só se tocam não conflitam).
- **Limite mensal**: se `area.limiteMensal != null`, conta reservas PENDENTE/CONFIRMADA do morador naquela área no mês da `data`.
- **Status inicial**: `requerAprovacao` → `PENDENTE`; senão com taxa > 0 → `PENDENTE` (aguardando pagamento); senão → `CONFIRMADA`.
- `DELETE /reservas/:id` (cancelar) e `PUT /reservas/:id/{aprovar,rejeitar}` só agem sobre reservas no status compatível; cancelar é permitido ao dono ou ao síndico do condomínio, aprovar/rejeitar só ao síndico.
- `DELETE /reservas/:id` responde **200** com a reserva atualizada (status `CANCELADA`), não 204 — é mudança de estado, não remoção.
- `POST /reservas/validar` roda todas as regras sem persistir e devolve `{ valido, motivo, taxa, requerPagamento, requerAprovacao, statusInicial }`.

### Limitações conhecidas do ReservasController (dependem do módulo de Pagamentos)

- Reserva com taxa nasce `PENDENTE` e **não há como confirmá-la** ainda (a confirmação virá do webhook do Mercado Pago). Hoje só um `PUT /reservas/:id/aprovar` do síndico a move para `CONFIRMADA`.
- Não há geração de QR Code Pix nem endpoint de pagamento.
- Sem envio de notificação em reserva confirmada/cancelada (Fase 3).
- `tabela reservas` é nova — criada pelo Hibernate (`ddl-auto=update`), não há entrada no `schema.sql`.

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
- **Testes automatizados (início)**: `SrvCondominioProApplicationTests.contextLoads` sobe o contexto inteiro contra H2 em memória (`src/test/resources/application.yaml`, sem tocar no Supabase nem no `schema.sql`); `ReservaServiceOverlapTest` cobre a lógica de sobreposição de horários (unit puro). O resto da verificação ainda é manual (curl) contra o Supabase real. Vale expandir para testes de integração (`@DataJpaTest`/`@WebMvcTest`) por módulo.
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
│   ├── areas/             (AreaComum, AreaComumRepository, AreaComumController, AreaComumService, dto/)
│   └── reservas/          (Reserva, StatusReserva, ReservaRepository, ReservaController, ReservaService, dto/)
└── shared/
    ├── exceptions/        (AppException, NotFoundException, ConflictException, ForbiddenException, GlobalExceptionHandler)
    └── security/          (JwtService, JwtFilter)
```
