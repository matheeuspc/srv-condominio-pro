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
- Sem `refund`/estorno ativo (só reflete o que vier do MP) e sem notificação de pagamento confirmado (Fase 3).
- Sem taxa da plataforma (2,5% + gateway do CONTEXT 2) — cobra-se apenas `area.taxa` cheia.

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
│   ├── reservas/          (Reserva, StatusReserva, ReservaRepository, ReservaController, ReservaService, dto/)
│   └── pagamentos/        (Pagamento, StatusPagamento, PagamentoRepository, PagamentoController, PagamentoService, dto/,
│                           mercadopago/ = MercadoPagoClient, MercadoPagoProperties, MercadoPagoConfig,
│                                          MercadoPagoWebhookValidator, MercadoPagoPayment, PagamentoPixRequest)
└── shared/
    ├── exceptions/        (AppException, NotFoundException, ConflictException, ForbiddenException, GlobalExceptionHandler)
    └── security/          (JwtService, JwtFilter)
```
