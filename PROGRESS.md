# Progresso do Backend — CondomínioPro

Acompanha a "Ordem de Implementação" definida em `CONTEXT.md` (seção 9). Atualizado em 2026-08-31.

> Fases 1–4 implementadas. Pós-roadmap: bloco de Auth do CONTEXT §6.1 completo e schema
> migrado para **Flyway** — ver "Pós-roadmap" abaixo.

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

- **Auth**: ~~`forgot-password`, `reset-password`, `refresh-token`~~ → implementados no pós-roadmap (ver seção própria).
- **Moradores**: ~~envio de convite por email ainda não existe~~ → Fase 3: `MoradorService.criar` agora dispara `notificacaoService.notificar(...)` com o token (best-effort). O `tokenConvite` continua na resposta como fallback quando nenhum canal está configurado.

## Fase 2 — Core do produto ✅

| # | Item | Status |
|---|------|--------|
| 6 | AreasController | ✅ |
| 7 | ReservasController | ✅ |
| 8 | PagamentosController (Mercado Pago + webhook) | ✅ (código; falta teste com credencial real de sandbox) |

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

- ~~Reserva com taxa nasce `PENDENTE` e não há como confirmá-la~~ → resolvido: `POST /pagamentos/webhook/mercadopago` move a reserva `PENDENTE` para `CONFIRMADA` quando o pagamento é aprovado.
- ~~Não há geração de QR Code Pix nem endpoint de pagamento~~ → resolvido: `POST /pagamentos/criar-cobranca`.
- Sem envio de notificação em reserva confirmada/cancelada (Fase 3).
- `tabela reservas` é nova — criada pelo Hibernate (`ddl-auto=update`), não há entrada no `schema.sql`.

### PagamentosController — regras implementadas (CONTEXT 5.2 / 6.7)

- `POST /pagamentos/criar-cobranca` (MORADOR, dono da reserva): exige reserva `PENDENTE` com `area.taxa > 0`; chama o Mercado Pago (`POST /v1/payments`, `payment_method_id: pix`) e persiste `Pagamento` com QR Code / `ticket_url`. Idempotente: se já existe cobrança `AGUARDANDO` com QR, devolve a mesma sem gerar outra; `PAGO` → 409.
- `GET /pagamentos/:id/status` e `GET /reservas/:reservaId/pagamento` (autenticado): dono da reserva ou síndico do condomínio.
- `GET /condominios/:condominioId/pagamentos` (SINDICO): relatório com `totalRecebido` (PAGO), `totalAguardando`, contagem e lista.
- `POST /pagamentos/webhook/mercadopago` (**público**, fora do JWT): valida `x-signature` (HMAC-SHA256 do manifesto `id;request-id;ts`); consulta o pagamento no MP (`GET /v1/payments/:id`) e mapeia status → `AGUARDANDO`/`PAGO`/`FALHOU`/`ESTORNADO`. `approved` → `Pagamento.PAGO` + `paidAt` + reserva `PENDENTE`→`CONFIRMADA`. Sempre responde 200 (exceto assinatura inválida → 403) para o MP não reenviar.
- Tabela `pagamentos` é nova — criada pelo Hibernate (`ddl-auto=update`), sem entrada no `schema.sql` (mesmo caso de `reservas`). Relação 1:1 com `reservas` (`reserva_id` UNIQUE).

### Config e limitações conhecidas do PagamentosController

- Config em `app.mercadopago.*` (`access-token`, `webhook-secret`, `base-url`, `notification-url`, `pix-expiration-minutes`). Sem `access-token`: `criar-cobranca` → 503 e webhook apenas logado. Sem `webhook-secret`: assinatura **não** é validada (só um `warn`). Credenciais reais vão em `application-local.yaml` (gitignored).
- Integração **ainda não testada contra o sandbox real do Mercado Pago** — falta `access-token` de teste. Só compila + `contextLoads` (H2) passam.
- Webhook `FALHOU`/`ESTORNADO` só atualiza o `Pagamento`; **não** cancela a reserva nem libera o horário (fica a cargo do síndico).
- Sem `refund`/estorno ativo (só reflete o que vier do MP).
- ~~sem notificação de pagamento confirmado~~ → Fase 3: `confirmarReserva` dispara notificação ao morador.
- Sem taxa da plataforma (2,5% + gateway do CONTEXT 2) — cobra-se apenas `area.taxa` cheia.

## Fase 3 — Comunicação ✅ (código; falta teste manual com provedores reais)

| # | Item | Status |
|---|------|--------|
| 9 | AvisosController | ✅ |
| 10 | FAQsController | ✅ |
| 11 | NotificacoesController (email + WhatsApp) | ✅ |

### AvisosController — regras implementadas (CONTEXT 5.3 / 6.8)

- Tabelas novas `avisos` e `aviso_leituras` — criadas pelo Hibernate (`ddl-auto=update`), sem entrada no `schema.sql` (mesmo critério de `reservas`/`pagamentos`).
- `POST /condominios/:id/avisos` (SINDICO): `destinatario` = `null` (todos), `PROPRIETARIO` ou `INQUILINO` (`SINDICO` → 400). Se `publicado`, dispara notificação "Novo comunicado" aos moradores **ativos** do segmento.
- `GET /condominios/:id/avisos`: SINDICO vê todos; morador vê só `publicado = true` e do seu segmento, cada item com flag `lido`.
- `GET /avisos/:id`: mesma visibilidade (morador sem acesso → 404, não 403, para não vazar existência).
- `PUT`/`DELETE /avisos/:id` (SINDICO). `DELETE` remove as `aviso_leituras` antes (sem FK órfã) e responde 204.
- `POST /avisos/:id/marcar-lido` (MORADOR): idempotente (204 mesmo se já lido).
- `GET /avisos/nao-lidos` (MORADOR): visíveis ao morador e ainda não lidos.

### FAQsController — regras implementadas (CONTEXT 5.4 / 6.9)

- Tabela nova `faqs` — Hibernate, sem `schema.sql`.
- `GET /condominios/:id/faqs` (autenticado): morador vê só `ativa = true`; SINDICO vê todas. Ordena por `ordem`, depois `id`. Filtro opcional `?categoria=` (extra, não estava no CONTEXT).
- `GET /condominios/:id/faqs/search?q=` (autenticado): `LIKE` case-insensitive em pergunta **ou** resposta; morador só recebe as ativas. `q` vazio → 400.
- `POST`/`PUT`/`DELETE` (SINDICO). `DELETE` é físico (o flag `ativa` é para ocultar sem apagar).

### NotificacoesController — regras implementadas (CONTEXT 5.5 / 6.10)

- Tabela `notificacoes` (CONTEXT 7) + `preferencias_notificacao` (nova, uma linha por morador) — ambas via Hibernate, sem `schema.sql`.
- `GET`/`PUT /moradores/me/preferencias-notificacoes` (MORADOR): upsert de `notificarEmail` (default `true`) e `notificarWhatsapp` (default `false`). Sem linha → devolve os defaults sem persistir.
- `GET /condominios/:id/notificacoes/log` (SINDICO): log de envios, mais recentes primeiro; filtros opcionais `?tipo=` e `?status=`.
- `POST /notificacoes/enviar` (SINDICO): alvo = `usuarioIds` (têm de ser do condomínio) **ou** `destinatario` (papel) **ou** todos os moradores ativos; `tipo` nulo segue a preferência de cada um. Sem canal configurado → 503. Responde `{ destinatarios, enviados, falhas }`.
- **Canais** (`NotificacaoService`): EMAIL via Resend (`POST /emails`), WHATSAPP via Twilio (`POST /Accounts/{sid}/Messages.json`, auth Basic). Cada envio grava uma `Notificacao` (`PENDENTE` → `ENVIADO`+`enviado_em` / `FALHOU`+`erro`).
- **Resolução de canais**: EMAIL sai se `pref.email` **e** Resend configurado. WHATSAPP sai se `pref.whatsapp` **e** `condominio.notifica_whatsapp` **e** Twilio configurado **e** o morador tem telefone.
- **Eventos que disparam notificação** (best-effort, nunca derrubam a transação de origem): novo comunicado publicado, reserva confirmada/cancelada/rejeitada (`ReservaService`), reserva confirmada por pagamento (`PagamentoService`), convite de morador (`MoradorService`).

### Config e limitações conhecidas da Fase 3

- Config em `app.resend.*` (`api-key`, `from`, `base-url`) e `app.twilio.*` (`account-sid`, `auth-token`, `from`, `base-url`). Credenciais reais vão em `application-local.yaml` (gitignored).
- **Sem provedor configurado, os eventos são no-op silencioso** (não gravam `Notificacao`); só `POST /notificacoes/enviar` reclama (503). Escolha deliberada para não poluir o log com falhas em ambiente sem credencial.
- **Não testado contra Resend/Twilio reais** — falta credencial. Passam: `mvn compile`, `contextLoads` (H2, valida todas as `@Query` e o wiring) e 3 testes unitários novos (`AvisoSegmentacaoTest`, `NotificacaoCanaisTest`, `WhatsappSenderE164Test`).
- Notificações **participam da transação do evento** que as originou (sem `@Async`/`REQUIRES_NEW`); um envio lento segura o request, igual às chamadas ao Mercado Pago. Se o evento fizer rollback depois do disparo, os registros de `Notificacao` também são revertidos.
- `normalizarE164` do WhatsApp é ingênua (assume Brasil / `+55` quando não há prefixo internacional).
- `POST /avisos/:id/marcar-lido` responde **204** (não devolve o aviso).
- Sem preferências por evento (só por canal) e sem digest/agrupamento — cada morador do segmento recebe uma notificação por comunicado.

## Fase 4 — Extras ✅ (código; falta teste manual contra Supabase)

| # | Item | Status |
|---|------|--------|
| 12 | RelatoriosController | ✅ |

### RelatoriosController — sem spec no CONTEXT.md (é "Extras"); endpoints definidos aqui

Todos **SINDICO**, sob `/api/v1/condominios/:condominioId/relatorios/*` — já cobertos pelo matcher `/api/v1/condominios/**` do `SecurityConfig` (**sem alteração de segurança**). Não há tabela nem coluna nova: cada relatório **agrega em memória** as listas que os repositórios dos outros módulos já expõem.

- `GET /relatorios/reservas?inicio=&fim=` — total, `porStatus` (todos os `StatusReserva`, zeros inclusos), `porMes` (`yyyy-MM` → contagem), `porArea` (todas as áreas do condomínio: total, confirmadas, `taxaArrecadada` = soma dos pagamentos `PAGO` das reservas da área no período).
- `GET /relatorios/pagamentos?inicio=&fim=` — `porStatus` (`{quantidade, valor}` por `StatusPagamento`), `totalRecebido`, `ticketMedio` (recebido ÷ qtd paga, 2 casas), `porMes` (`{quantidade, recebido}`), `porArea` (`{pagos, recebido}`). Janela filtrada por `pagamento.created_at`.
- `GET /relatorios/ocupacao` — snapshot **sem período**: unidades total/ocupadas/vazias (ocupada = tem vínculo `MoradorUnidade` `ATIVO`), moradores ativos por tipo, áreas total/ativas.
- `GET /relatorios/comunicacao?inicio=&fim=` — `avisosPublicados` no período com `{elegiveis, leituras, taxaLeitura}` por aviso (elegíveis = moradores ativos do segmento), e `notificacoes` agregadas por `tipo`×`status` (só combinações com contagem > 0).

**Parâmetros de período**: `inicio`/`fim` ISO (`yyyy-MM-dd`), **opcionais**. Default = primeiro dia de 12 meses atrás até hoje. Só `fim` → `inicio` recua 12 meses. `inicio > fim` → 400.

### Limitações conhecidas do RelatoriosController

- **Agrega em memória** (carrega todas as reservas/pagamentos/avisos/notificações do condomínio e filtra em Java, igual ao `PagamentoService.relatorio`). Para históricos muito grandes vale mover a agregação para o banco (`GROUP BY`).
- Métodos anotados `@Transactional(readOnly = true)` — primeiro uso desse padrão no projeto; garante sessão aberta para a navegação lazy pesada entre entidades, sem depender do OSIV.
- 2 métodos de repositório novos: `AvisoLeituraRepository.countByAvisoId`, `MoradorUnidadeRepository.findByUnidadeCondominioIdAndStatus`.
- **Não testado contra o Supabase real** — só `mvn compile`, `contextLoads` (H2, valida os derived queries e o wiring) e `RelatorioPeriodoTest` (unit puro de `resolverPeriodo`/`chaveMes`).
- `dashboard` do `CondominioController` (Fase 1, 2 contadores) e `GET /condominios/:id/pagamentos` (Fase 2, relatório financeiro simples) **continuam existindo** — não foram substituídos.

---

## Pós-roadmap

### Auth §6.1 — completo

`AuthController` agora tem os 3 endpoints públicos que faltavam:

- `POST /auth/forgot-password` `{ email }` → **sempre 200** com mensagem neutra (não revela se o email existe). Se o usuário existe e está ativo, grava `token_reset_senha` + `token_reset_expiracao` (**1 hora**) e dispara `notificacaoService.notificar(...)` com o token (best-effort, mesmo padrão do convite).
- `POST /auth/reset-password` `{ token, senha }` (`senha` min. 8) → valida token não expirado + usuário ativo, troca a senha (BCrypt), limpa os campos de reset e **já devolve um `AuthResponse`** (JWT novo), igual ao aceite de convite. Token inválido/expirado → 400.
- `POST /auth/refresh-token` `{ token }` → renova um JWT **ainda válido** (sessão deslizante) e devolve `AuthResponse`. Token inválido/expirado ou usuário inativo → 401.

**Decisões**: campos de reset **separados** do `token_convite` (não colidem com convite pendente); reset e refresh reaproveitam o fluxo "devolve JWT" já existente; `AuthService` passou a depender de `NotificacaoService` (sem ciclo). **Limitação**: não há refresh token de verdade — um JWT já expirado exige `login`. Teste novo: `AuthTokenExpiracaoTest` (unit puro de `AuthService.tokenValido`).

### Migrations — Flyway

Schema saiu de `ddl-auto=update` + `schema.sql` para **Flyway** (`flyway-core` + `flyway-database-postgresql`, versões pelo BOM do Spring Boot). Migrations em `src/main/resources/db/migration/`:

- `V1__baseline_schema.sql` — schema completo das Fases 1-4 (tipos espelhando as anotações JPA). É o ponto de partida para **banco vazio** (CI / ambiente novo).
- `V2__auth_token_reset_senha.sql` — as 2 colunas de reset em `usuarios` + constraint unique.

**Config** (`application.yaml` de produção):
- `spring.flyway.baseline-on-migrate: true`, `baseline-version: 1` → a instância Supabase (que já tem as tabelas via `ddl-auto`) é **carimbada como V1 sem rodar o V1**; o Flyway aplica só da **V2 em diante**.
- `spring.jpa.hibernate.ddl-auto: none` (era `update`) — Flyway é a fonte da verdade; `none` (em vez de `validate`) evita quebra de boot por drift entre o V1 escrito à mão e o que o `ddl-auto` criou incrementalmente no Supabase. **Promover a `validate` depois de confirmar o V1 contra o banco real.**
- `spring.sql.init.mode: never` (era `always`) — `schema.sql` **aposentado** (arquivo mantido no repo só como histórico).
- Perfil de teste (`src/test/resources/application.yaml`): `spring.flyway.enabled: false` + `ddl-auto: create-drop` — o `contextLoads` continua montando o schema pelo Hibernate no H2 (as migrations são PostgreSQL puro).

**Não verificado contra Postgres real** — `mvn test` (25, H2, Flyway off) passa, mas o primeiro boot contra o Supabase precisa ser acompanhado: é quando o Flyway cria o `flyway_schema_history`, carimba o baseline e roda a V2. Se a V2 falhar (ex.: coluna já criada por um boot anterior com `ddl-auto`), remover o `ADD COLUMN` correspondente da V2 e usar `flyway repair` / marcar como aplicada.

---

## Notas de infraestrutura

- **Banco**: Postgres gerenciado pelo Supabase (não é um banco local/efêmero — é a instância real do projeto).
- **Schema**: gerido por **Flyway** (`src/main/resources/db/migration/`, ver "Pós-roadmap"). `ddl-auto: none`. O `schema.sql` legado (patch camelCase→snake_case da instância Supabase) está **aposentado** — arquivo mantido só como histórico, não roda mais (`spring.sql.init.mode: never`).
- **Credenciais**: `application.yaml` só tem defaults seguros (placeholders locais); as credenciais reais do Supabase ficam em `src/main/resources/application-local.yaml`, que está no `.gitignore` — rodar com `--spring-boot.run.profiles=local` (ou `SPRING_PROFILES_ACTIVE=local`) localmente.
- **Testes automatizados (início)**: `SrvCondominioProApplicationTests.contextLoads` sobe o contexto inteiro contra H2 em memória (`src/test/resources/application.yaml`, sem Supabase, sem `schema.sql`, sem Flyway); unit puro: `ReservaServiceOverlapTest` (sobreposição de horários), `AvisoSegmentacaoTest` (visibilidade de aviso por papel), `NotificacaoCanaisTest` (resolução de canais), `WhatsappSenderE164Test` (normalização de telefone), `RelatorioPeriodoTest` (janela de período), `AuthTokenExpiracaoTest` (expiração de token). **25 testes** no total. O resto da verificação ainda é manual (curl) contra o Supabase real. Vale expandir para testes de integração (`@DataJpaTest`/`@WebMvcTest`) por módulo.
- **Dado legado preservado**: existe um condomínio real "Residencial das Flores" (id=1) com 2 unidades, que já estava no banco antes deste trabalho começar — foi preservado ao corrigir o `schema.sql`, mas perdeu metadados de baixo valor (created_at/updated_at originais, preferências de notificação) durante uma correção de schema anterior a eu saber que era dado real.
- **Dados de teste**: condomínio "Condomínio Teste Auth ATUALIZADO" (id=3) e usuários/moradores/áreas/unidades associados foram criados durante os testes manuais e continuam no banco — ainda não foram limpos.

## Estrutura de pacotes atual

```
com.mcardoso.srvcondominiopro/
├── config/
│   └── SecurityConfig.java
├── modules/
│   ├── auth/            (AuthController, AuthService, dto/ = register/login/me + forgot/reset/refresh)
│   ├── condominios/      (Condominio, CondominioRepository, CondominioController, CondominioService, dto/)
│   ├── usuarios/          (Usuario, Role, UsuarioRepository)
│   ├── unidades/          (Unidade, UnidadeRepository, UnidadeController, UnidadeService, dto/)
│   ├── moradores/         (MoradorUnidade, StatusMorador, MoradorUnidadeRepository, MoradorController, MoradorService, dto/)
│   ├── areas/             (AreaComum, AreaComumRepository, AreaComumController, AreaComumService, dto/)
│   ├── reservas/          (Reserva, StatusReserva, ReservaRepository, ReservaController, ReservaService, dto/)
│   ├── pagamentos/        (Pagamento, StatusPagamento, PagamentoRepository, PagamentoController, PagamentoService, dto/,
│   │                       mercadopago/ = MercadoPagoClient, MercadoPagoProperties, MercadoPagoConfig,
│   │                                      MercadoPagoWebhookValidator, MercadoPagoPayment, PagamentoPixRequest)
│   ├── avisos/            (Aviso, AvisoLeitura, AvisoRepository, AvisoLeituraRepository,
│   │                       AvisoController, AvisoService, dto/)
│   ├── faqs/              (Faq, CategoriaFaq, FaqRepository, FaqController, FaqService, dto/)
│   ├── notificacoes/      (Notificacao, PreferenciaNotificacao, TipoNotificacao, StatusNotificacao,
│   │                       NotificacaoRepository, PreferenciaNotificacaoRepository,
│   │                       NotificacaoController, NotificacaoService, dto/,
│   │                       canais/ = EmailSender (Resend), WhatsappSender (Twilio),
│   │                                 ResendProperties, TwilioProperties, NotificacaoCanaisConfig)
│   └── relatorios/        (RelatorioController, RelatorioService, dto/) — só agrega, sem entidade
├── shared/
│   ├── exceptions/        (AppException, NotFoundException, ConflictException, ForbiddenException, GlobalExceptionHandler)
│   └── security/          (JwtService, JwtFilter)
└── resources/db/migration/  (V1__baseline_schema.sql, V2__auth_token_reset_senha.sql)
```
