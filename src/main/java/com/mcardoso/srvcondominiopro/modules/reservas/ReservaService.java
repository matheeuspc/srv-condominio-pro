package com.mcardoso.srvcondominiopro.modules.reservas;

import com.mcardoso.srvcondominiopro.modules.areas.AreaComum;
import com.mcardoso.srvcondominiopro.modules.areas.AreaComumRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.MoradorUnidadeRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.StatusMorador;
import com.mcardoso.srvcondominiopro.modules.notificacoes.NotificacaoService;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.CreateReservaRequest;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.RejeitarReservaRequest;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.ReservaResponse;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.ValidacaoReservaResponse;
import com.mcardoso.srvcondominiopro.modules.unidades.Unidade;
import com.mcardoso.srvcondominiopro.modules.unidades.UnidadeRepository;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ConflictException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReservaService {

    /** Reservas que ocupam um horário: bloqueiam conflito e contam para o limite mensal. */
    private static final List<StatusReserva> STATUS_ATIVOS =
            List.of(StatusReserva.PENDENTE, StatusReserva.CONFIRMADA);

    private final ReservaRepository reservaRepository;
    private final AreaComumRepository areaComumRepository;
    private final UnidadeRepository unidadeRepository;
    private final MoradorUnidadeRepository moradorUnidadeRepository;
    private final NotificacaoService notificacaoService;

    public ReservaService(
            ReservaRepository reservaRepository,
            AreaComumRepository areaComumRepository,
            UnidadeRepository unidadeRepository,
            MoradorUnidadeRepository moradorUnidadeRepository,
            NotificacaoService notificacaoService
    ) {
        this.reservaRepository = reservaRepository;
        this.areaComumRepository = areaComumRepository;
        this.unidadeRepository = unidadeRepository;
        this.moradorUnidadeRepository = moradorUnidadeRepository;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public ReservaResponse criar(CreateReservaRequest request, Usuario moradorLogado) {
        Contexto ctx = validarCriacao(request, moradorLogado);

        Reserva reserva = new Reserva();
        reserva.setArea(ctx.area());
        reserva.setUnidade(ctx.unidade());
        reserva.setUsuario(moradorLogado);
        reserva.setData(request.data());
        reserva.setHoraInicio(request.horaInicio());
        reserva.setHoraFim(request.horaFim());
        reserva.setObservacao(request.observacao());
        reserva.setStatus(statusInicial(ctx.area()));
        reservaRepository.save(reserva);

        return ReservaResponse.from(reserva);
    }

    public ValidacaoReservaResponse validar(CreateReservaRequest request, Usuario moradorLogado) {
        try {
            Contexto ctx = validarCriacao(request, moradorLogado);
            AreaComum area = ctx.area();
            return ValidacaoReservaResponse.valido(
                    area.getTaxa(),
                    temTaxa(area),
                    area.isRequerAprovacao(),
                    statusInicial(area)
            );
        } catch (AppException ex) {
            return ValidacaoReservaResponse.invalido(ex.getMessage());
        }
    }

    public ReservaResponse buscar(Long id, Usuario usuarioLogado) {
        Reserva reserva = buscarPorId(id);
        validarAcessoReserva(reserva, usuarioLogado);
        return ReservaResponse.from(reserva);
    }

    public List<ReservaResponse> minhasReservas(Usuario moradorLogado) {
        return reservaRepository.findByUsuarioIdOrderByDataDescHoraInicioDesc(moradorLogado.getId()).stream()
                .map(ReservaResponse::from)
                .toList();
    }

    public List<ReservaResponse> listarPorArea(Long areaId, Usuario usuarioLogado) {
        AreaComum area = areaComumRepository.findById(areaId)
                .orElseThrow(() -> new NotFoundException("Área comum não encontrada"));
        validarAcessoCondominio(area.getCondominio().getId(), usuarioLogado);
        return reservaRepository.findByAreaIdOrderByDataDescHoraInicioDesc(areaId).stream()
                .map(ReservaResponse::from)
                .toList();
    }

    public List<ReservaResponse> listarPorCondominio(Long condominioId, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        return reservaRepository.findByAreaCondominioIdOrderByDataDescHoraInicioDesc(condominioId).stream()
                .map(ReservaResponse::from)
                .toList();
    }

    @Transactional
    public ReservaResponse cancelar(Long id, Usuario usuarioLogado) {
        Reserva reserva = buscarPorId(id);
        validarAcessoReserva(reserva, usuarioLogado);

        if (reserva.getStatus() == StatusReserva.CANCELADA || reserva.getStatus() == StatusReserva.REJEITADA) {
            throw new AppException("Reserva não pode ser cancelada no status atual", HttpStatus.BAD_REQUEST);
        }

        reserva.setStatus(StatusReserva.CANCELADA);
        notificarMorador(reserva, "cancelada");
        return ReservaResponse.from(reserva);
    }

    @Transactional
    public ReservaResponse aprovar(Long id, Usuario sindicoLogado) {
        Reserva reserva = buscarPorId(id);
        validarAcessoCondominio(reserva.getArea().getCondominio().getId(), sindicoLogado);

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new AppException("Só é possível aprovar reservas pendentes", HttpStatus.BAD_REQUEST);
        }

        reserva.setStatus(StatusReserva.CONFIRMADA);
        notificarMorador(reserva, "confirmada");
        return ReservaResponse.from(reserva);
    }

    @Transactional
    public ReservaResponse rejeitar(Long id, RejeitarReservaRequest request, Usuario sindicoLogado) {
        Reserva reserva = buscarPorId(id);
        validarAcessoCondominio(reserva.getArea().getCondominio().getId(), sindicoLogado);

        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new AppException("Só é possível rejeitar reservas pendentes", HttpStatus.BAD_REQUEST);
        }

        reserva.setStatus(StatusReserva.REJEITADA);
        if (request != null && request.motivo() != null && !request.motivo().isBlank()) {
            reserva.setObservacao(request.motivo());
        }
        notificarMorador(reserva, "rejeitada");
        return ReservaResponse.from(reserva);
    }

    // --- regras de negócio ---

    private void notificarMorador(Reserva reserva, String situacao) {
        notificacaoService.notificar(
                reserva.getUsuario(),
                "Reserva " + situacao,
                "Sua reserva da área %s em %s (%s–%s) foi %s.".formatted(
                        reserva.getArea().getNome(), reserva.getData(),
                        reserva.getHoraInicio(), reserva.getHoraFim(), situacao));
    }

    private Contexto validarCriacao(CreateReservaRequest request, Usuario moradorLogado) {
        AreaComum area = areaComumRepository.findById(request.areaId())
                .orElseThrow(() -> new NotFoundException("Área comum não encontrada"));
        if (!area.isAtiva()) {
            throw new AppException("Área não está disponível para reservas", HttpStatus.BAD_REQUEST);
        }
        validarAcessoCondominio(area.getCondominio().getId(), moradorLogado);

        Unidade unidade = unidadeRepository.findById(request.unidadeId())
                .orElseThrow(() -> new NotFoundException("Unidade não encontrada"));
        if (!unidade.getCondominio().getId().equals(area.getCondominio().getId())) {
            throw new ForbiddenException("A unidade não pertence ao mesmo condomínio da área");
        }
        if (!moradorUnidadeRepository.existsByUsuarioIdAndUnidadeIdAndStatus(
                moradorLogado.getId(), unidade.getId(), StatusMorador.ATIVO)) {
            throw new ForbiddenException("Você não está vinculado a esta unidade");
        }

        LocalTime inicio = parseHora(request.horaInicio());
        LocalTime fim = parseHora(request.horaFim());
        if (!inicio.isBefore(fim)) {
            throw new AppException("Hora de início deve ser antes da hora de fim", HttpStatus.BAD_REQUEST);
        }

        LocalTime aberturaArea = parseHora(area.getHorarioInicio());
        LocalTime fechamentoArea = parseHora(area.getHorarioFim());
        if (inicio.isBefore(aberturaArea) || fim.isAfter(fechamentoArea)) {
            throw new AppException(
                    "Horário fora do funcionamento da área (%s às %s)"
                            .formatted(area.getHorarioInicio(), area.getHorarioFim()),
                    HttpStatus.BAD_REQUEST);
        }

        validarAntecedencia(area, request.data());
        validarConflito(area.getId(), request.data(), inicio, fim);
        validarLimiteMensal(area, moradorLogado.getId(), request.data());

        return new Contexto(area, unidade);
    }

    private void validarAntecedencia(AreaComum area, LocalDate data) {
        LocalDate hoje = LocalDate.now();
        LocalDate minima = hoje.plusDays(area.getAntecedenciaMin());
        LocalDate maxima = hoje.plusDays(area.getAntecedenciaMax());

        if (data.isBefore(minima)) {
            throw new AppException(
                    "Reserva exige antecedência mínima de %d dia(s)".formatted(area.getAntecedenciaMin()),
                    HttpStatus.BAD_REQUEST);
        }
        if (data.isAfter(maxima)) {
            throw new AppException(
                    "Reserva permitida com no máximo %d dia(s) de antecedência".formatted(area.getAntecedenciaMax()),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void validarConflito(Long areaId, LocalDate data, LocalTime inicio, LocalTime fim) {
        List<Reserva> doDia = reservaRepository.findByAreaIdAndDataAndStatusIn(areaId, data, STATUS_ATIVOS);
        for (Reserva existente : doDia) {
            if (intervalosSobrepoem(inicio, fim,
                    parseHora(existente.getHoraInicio()), parseHora(existente.getHoraFim()))) {
                throw new ConflictException("Já existe uma reserva para este horário");
            }
        }
    }

    private void validarLimiteMensal(AreaComum area, Long usuarioId, LocalDate data) {
        if (area.getLimiteMensal() == null) {
            return;
        }
        LocalDate primeiroDia = data.withDayOfMonth(1);
        LocalDate ultimoDia = data.withDayOfMonth(data.lengthOfMonth());
        long jaReservadas = reservaRepository.countByUsuarioIdAndAreaIdAndDataBetweenAndStatusIn(
                usuarioId, area.getId(), primeiroDia, ultimoDia, STATUS_ATIVOS);
        if (jaReservadas >= area.getLimiteMensal()) {
            throw new ConflictException(
                    "Limite mensal de %d reserva(s) para esta área atingido".formatted(area.getLimiteMensal()));
        }
    }

    private StatusReserva statusInicial(AreaComum area) {
        if (area.isRequerAprovacao()) {
            return StatusReserva.PENDENTE;
        }
        // Com taxa: nasce PENDENTE e só é confirmada quando o pagamento cair
        // (webhook do Mercado Pago — módulo de Pagamentos, ainda não implementado).
        if (temTaxa(area)) {
            return StatusReserva.PENDENTE;
        }
        return StatusReserva.CONFIRMADA;
    }

    private static boolean temTaxa(AreaComum area) {
        return area.getTaxa() != null && area.getTaxa().compareTo(BigDecimal.ZERO) > 0;
    }

    static boolean intervalosSobrepoem(LocalTime aInicio, LocalTime aFim, LocalTime bInicio, LocalTime bFim) {
        return aInicio.isBefore(bFim) && bInicio.isBefore(aFim);
    }

    private static LocalTime parseHora(String valor) {
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeException ex) {
            throw new AppException("Horário inválido, use o formato HH:mm", HttpStatus.BAD_REQUEST);
        }
    }

    private Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada"));
    }

    private void validarAcessoReserva(Reserva reserva, Usuario usuarioLogado) {
        if (usuarioLogado.getRole() == Role.SINDICO) {
            validarAcessoCondominio(reserva.getArea().getCondominio().getId(), usuarioLogado);
            return;
        }
        if (!reserva.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta reserva");
        }
    }

    private void validarAcessoCondominio(Long condominioId, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }

    private record Contexto(AreaComum area, Unidade unidade) {
    }
}
