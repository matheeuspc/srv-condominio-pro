package com.mcardoso.srvcondominiopro.modules.relatorios;

import com.mcardoso.srvcondominiopro.modules.areas.AreaComum;
import com.mcardoso.srvcondominiopro.modules.areas.AreaComumRepository;
import com.mcardoso.srvcondominiopro.modules.avisos.Aviso;
import com.mcardoso.srvcondominiopro.modules.avisos.AvisoLeituraRepository;
import com.mcardoso.srvcondominiopro.modules.avisos.AvisoRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.MoradorUnidadeRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.StatusMorador;
import com.mcardoso.srvcondominiopro.modules.notificacoes.Notificacao;
import com.mcardoso.srvcondominiopro.modules.notificacoes.NotificacaoRepository;
import com.mcardoso.srvcondominiopro.modules.notificacoes.StatusNotificacao;
import com.mcardoso.srvcondominiopro.modules.notificacoes.TipoNotificacao;
import com.mcardoso.srvcondominiopro.modules.pagamentos.Pagamento;
import com.mcardoso.srvcondominiopro.modules.pagamentos.PagamentoRepository;
import com.mcardoso.srvcondominiopro.modules.pagamentos.StatusPagamento;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioComunicacaoResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioComunicacaoResponse.AvisoEngajamento;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioComunicacaoResponse.NotificacaoAgregada;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioOcupacaoResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioPagamentosResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioPagamentosResponse.PagamentosPorArea;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioPagamentosResponse.StatusItem;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioPagamentosResponse.ValorMensal;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioReservasResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioReservasResponse.ReservasPorArea;
import com.mcardoso.srvcondominiopro.modules.reservas.Reserva;
import com.mcardoso.srvcondominiopro.modules.reservas.ReservaRepository;
import com.mcardoso.srvcondominiopro.modules.reservas.StatusReserva;
import com.mcardoso.srvcondominiopro.modules.unidades.Unidade;
import com.mcardoso.srvcondominiopro.modules.unidades.UnidadeRepository;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

// Relatórios consolidados para o síndico (Fase 4). Cada método agrega em memória as listas
// já expostas pelos repositórios dos outros módulos — não há tabelas nem colunas novas.
@Service
public class RelatorioService {

    private final ReservaRepository reservaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final AreaComumRepository areaComumRepository;
    private final AvisoRepository avisoRepository;
    private final AvisoLeituraRepository avisoLeituraRepository;
    private final NotificacaoRepository notificacaoRepository;
    private final UnidadeRepository unidadeRepository;
    private final UsuarioRepository usuarioRepository;
    private final MoradorUnidadeRepository moradorUnidadeRepository;

    public RelatorioService(
            ReservaRepository reservaRepository,
            PagamentoRepository pagamentoRepository,
            AreaComumRepository areaComumRepository,
            AvisoRepository avisoRepository,
            AvisoLeituraRepository avisoLeituraRepository,
            NotificacaoRepository notificacaoRepository,
            UnidadeRepository unidadeRepository,
            UsuarioRepository usuarioRepository,
            MoradorUnidadeRepository moradorUnidadeRepository
    ) {
        this.reservaRepository = reservaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.areaComumRepository = areaComumRepository;
        this.avisoRepository = avisoRepository;
        this.avisoLeituraRepository = avisoLeituraRepository;
        this.notificacaoRepository = notificacaoRepository;
        this.unidadeRepository = unidadeRepository;
        this.usuarioRepository = usuarioRepository;
        this.moradorUnidadeRepository = moradorUnidadeRepository;
    }

    // ------------------------------------------------------------------
    // Reservas no período
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public RelatorioReservasResponse reservas(Long condominioId, LocalDate inicio, LocalDate fim, Usuario sindico) {
        validarAcesso(condominioId, sindico);
        Periodo periodo = resolverPeriodo(inicio, fim, LocalDate.now());

        List<Reserva> reservas = reservaRepository
                .findByAreaCondominioIdOrderByDataDescHoraInicioDesc(condominioId).stream()
                .filter(r -> dentroDoPeriodo(r.getData(), periodo))
                .toList();

        Map<StatusReserva, Long> porStatus = new EnumMap<>(StatusReserva.class);
        for (StatusReserva s : StatusReserva.values()) {
            porStatus.put(s, 0L);
        }
        Map<String, Long> porMes = new TreeMap<>();
        for (Reserva r : reservas) {
            porStatus.merge(r.getStatus(), 1L, Long::sum);
            porMes.merge(chaveMes(r.getData()), 1L, Long::sum);
        }

        // Taxa efetivamente arrecadada (pagamentos PAGO) por área, dentro do mesmo período.
        Map<Long, BigDecimal> arrecadadoPorArea = new HashMap<>();
        for (Pagamento p : pagamentoRepository.findByReservaAreaCondominioIdOrderByCreatedAtDesc(condominioId)) {
            if (p.getStatus() != StatusPagamento.PAGO) {
                continue;
            }
            Reserva r = p.getReserva();
            if (!dentroDoPeriodo(r.getData(), periodo)) {
                continue;
            }
            arrecadadoPorArea.merge(r.getArea().getId(), p.getValor(), BigDecimal::add);
        }

        List<ReservasPorArea> porArea = new ArrayList<>();
        for (AreaComum area : areaComumRepository.findByCondominioIdOrderByNomeAsc(condominioId)) {
            List<Reserva> daArea = reservas.stream()
                    .filter(r -> r.getArea().getId().equals(area.getId()))
                    .toList();
            long confirmadas = daArea.stream()
                    .filter(r -> r.getStatus() == StatusReserva.CONFIRMADA)
                    .count();
            porArea.add(new ReservasPorArea(
                    area.getId(),
                    area.getNome(),
                    daArea.size(),
                    confirmadas,
                    arrecadadoPorArea.getOrDefault(area.getId(), BigDecimal.ZERO)));
        }

        return new RelatorioReservasResponse(
                condominioId, periodo.inicio(), periodo.fim(), reservas.size(), porStatus, porMes, porArea);
    }

    // ------------------------------------------------------------------
    // Pagamentos no período
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public RelatorioPagamentosResponse pagamentos(Long condominioId, LocalDate inicio, LocalDate fim, Usuario sindico) {
        validarAcesso(condominioId, sindico);
        Periodo periodo = resolverPeriodo(inicio, fim, LocalDate.now());

        List<Pagamento> pagamentos = pagamentoRepository
                .findByReservaAreaCondominioIdOrderByCreatedAtDesc(condominioId).stream()
                .filter(p -> dentroDoPeriodo(p.getCreatedAt().toLocalDate(), periodo))
                .toList();

        Map<StatusPagamento, Long> qtdPorStatus = new EnumMap<>(StatusPagamento.class);
        Map<StatusPagamento, BigDecimal> valorPorStatus = new EnumMap<>(StatusPagamento.class);
        for (StatusPagamento s : StatusPagamento.values()) {
            qtdPorStatus.put(s, 0L);
            valorPorStatus.put(s, BigDecimal.ZERO);
        }
        Map<String, Long> qtdMes = new TreeMap<>();
        Map<String, BigDecimal> recebidoMes = new TreeMap<>();
        Map<Long, BigDecimal> recebidoArea = new HashMap<>();
        Map<Long, Long> pagosArea = new HashMap<>();

        for (Pagamento p : pagamentos) {
            qtdPorStatus.merge(p.getStatus(), 1L, Long::sum);
            valorPorStatus.merge(p.getStatus(), p.getValor(), BigDecimal::add);

            String mes = chaveMes(p.getCreatedAt().toLocalDate());
            qtdMes.merge(mes, 1L, Long::sum);

            if (p.getStatus() == StatusPagamento.PAGO) {
                recebidoMes.merge(mes, p.getValor(), BigDecimal::add);
                Long areaId = p.getReserva().getArea().getId();
                recebidoArea.merge(areaId, p.getValor(), BigDecimal::add);
                pagosArea.merge(areaId, 1L, Long::sum);
            }
        }

        Map<StatusPagamento, StatusItem> porStatus = new EnumMap<>(StatusPagamento.class);
        for (StatusPagamento s : StatusPagamento.values()) {
            porStatus.put(s, new StatusItem(qtdPorStatus.get(s), valorPorStatus.get(s)));
        }

        Map<String, ValorMensal> porMes = new TreeMap<>();
        for (Map.Entry<String, Long> entrada : qtdMes.entrySet()) {
            porMes.put(entrada.getKey(), new ValorMensal(
                    entrada.getValue(), recebidoMes.getOrDefault(entrada.getKey(), BigDecimal.ZERO)));
        }

        BigDecimal totalRecebido = valorPorStatus.get(StatusPagamento.PAGO);
        long qtdPagos = qtdPorStatus.get(StatusPagamento.PAGO);
        BigDecimal ticketMedio = qtdPagos == 0
                ? BigDecimal.ZERO
                : totalRecebido.divide(BigDecimal.valueOf(qtdPagos), 2, RoundingMode.HALF_UP);

        List<PagamentosPorArea> porArea = new ArrayList<>();
        for (AreaComum area : areaComumRepository.findByCondominioIdOrderByNomeAsc(condominioId)) {
            porArea.add(new PagamentosPorArea(
                    area.getId(),
                    area.getNome(),
                    pagosArea.getOrDefault(area.getId(), 0L),
                    recebidoArea.getOrDefault(area.getId(), BigDecimal.ZERO)));
        }

        return new RelatorioPagamentosResponse(
                condominioId, periodo.inicio(), periodo.fim(), porStatus, totalRecebido, ticketMedio, porMes, porArea);
    }

    // ------------------------------------------------------------------
    // Ocupação (snapshot atual, sem período)
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public RelatorioOcupacaoResponse ocupacao(Long condominioId, Usuario sindico) {
        validarAcesso(condominioId, sindico);

        List<Unidade> unidades = unidadeRepository.findByCondominioIdOrderByBlocoAscNumeroAsc(condominioId);
        Set<Long> unidadesOcupadas = new HashSet<>();
        moradorUnidadeRepository.findByUnidadeCondominioIdAndStatus(condominioId, StatusMorador.ATIVO)
                .forEach(vinculo -> unidadesOcupadas.add(vinculo.getUnidade().getId()));

        long proprietarios = usuarioRepository
                .countByCondominioIdAndAtivoTrueAndRoleIn(condominioId, List.of(Role.PROPRIETARIO));
        long inquilinos = usuarioRepository
                .countByCondominioIdAndAtivoTrueAndRoleIn(condominioId, List.of(Role.INQUILINO));

        List<AreaComum> areas = areaComumRepository.findByCondominioIdOrderByNomeAsc(condominioId);
        long areasAtivas = areas.stream().filter(AreaComum::isAtiva).count();

        long total = unidades.size();
        long ocupadas = unidades.stream().filter(u -> unidadesOcupadas.contains(u.getId())).count();

        return new RelatorioOcupacaoResponse(
                condominioId,
                total,
                ocupadas,
                total - ocupadas,
                proprietarios + inquilinos,
                proprietarios,
                inquilinos,
                areas.size(),
                areasAtivas);
    }

    // ------------------------------------------------------------------
    // Comunicação (avisos + notificações no período)
    // ------------------------------------------------------------------
    @Transactional(readOnly = true)
    public RelatorioComunicacaoResponse comunicacao(Long condominioId, LocalDate inicio, LocalDate fim, Usuario sindico) {
        validarAcesso(condominioId, sindico);
        Periodo periodo = resolverPeriodo(inicio, fim, LocalDate.now());

        long proprietarios = usuarioRepository
                .countByCondominioIdAndAtivoTrueAndRoleIn(condominioId, List.of(Role.PROPRIETARIO));
        long inquilinos = usuarioRepository
                .countByCondominioIdAndAtivoTrueAndRoleIn(condominioId, List.of(Role.INQUILINO));

        List<AvisoEngajamento> avisos = new ArrayList<>();
        for (Aviso aviso : avisoRepository.findByCondominioIdOrderByCreatedAtDesc(condominioId)) {
            if (!aviso.isPublicado() || !dentroDoPeriodo(aviso.getCreatedAt().toLocalDate(), periodo)) {
                continue;
            }
            long elegiveis = elegiveis(aviso.getDestinatario(), proprietarios, inquilinos);
            long leituras = avisoLeituraRepository.countByAvisoId(aviso.getId());
            double taxaLeitura = elegiveis == 0
                    ? 0.0
                    : BigDecimal.valueOf(leituras)
                            .divide(BigDecimal.valueOf(elegiveis), 4, RoundingMode.HALF_UP)
                            .doubleValue();
            avisos.add(new AvisoEngajamento(
                    aviso.getId(), aviso.getTitulo(), aviso.getDestinatario(), elegiveis, leituras, taxaLeitura));
        }

        List<Notificacao> notificacoesDoPeriodo = notificacaoRepository
                .findByUsuarioCondominioIdOrderByCreatedAtDesc(condominioId).stream()
                .filter(n -> dentroDoPeriodo(n.getCreatedAt().toLocalDate(), periodo))
                .toList();
        List<NotificacaoAgregada> notificacoes = new ArrayList<>();
        for (TipoNotificacao tipo : TipoNotificacao.values()) {
            for (StatusNotificacao status : StatusNotificacao.values()) {
                long quantidade = notificacoesDoPeriodo.stream()
                        .filter(n -> n.getTipo() == tipo && n.getStatus() == status)
                        .count();
                if (quantidade > 0) {
                    notificacoes.add(new NotificacaoAgregada(tipo, status, quantidade));
                }
            }
        }

        return new RelatorioComunicacaoResponse(
                condominioId, periodo.inicio(), periodo.fim(), avisos.size(), avisos, notificacoes);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    record Periodo(LocalDate inicio, LocalDate fim) {
    }

    /** Período default: do primeiro dia de 12 meses atrás até hoje. `inicio`/`fim` sobrescrevem. */
    static Periodo resolverPeriodo(LocalDate inicio, LocalDate fim, LocalDate hoje) {
        LocalDate fimEfetivo = fim != null ? fim : hoje;
        LocalDate inicioEfetivo = inicio != null ? inicio : fimEfetivo.minusMonths(12).withDayOfMonth(1);
        if (inicioEfetivo.isAfter(fimEfetivo)) {
            throw new AppException("inicio deve ser anterior ou igual a fim", HttpStatus.BAD_REQUEST);
        }
        return new Periodo(inicioEfetivo, fimEfetivo);
    }

    static String chaveMes(LocalDate data) {
        return "%04d-%02d".formatted(data.getYear(), data.getMonthValue());
    }

    private static long elegiveis(Role destinatario, long proprietarios, long inquilinos) {
        if (destinatario == Role.PROPRIETARIO) {
            return proprietarios;
        }
        if (destinatario == Role.INQUILINO) {
            return inquilinos;
        }
        return proprietarios + inquilinos;
    }

    private static boolean dentroDoPeriodo(LocalDate data, Periodo periodo) {
        return data != null && !data.isBefore(periodo.inicio()) && !data.isAfter(periodo.fim());
    }

    private void validarAcesso(Long condominioId, Usuario sindico) {
        if (!sindico.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }
}
