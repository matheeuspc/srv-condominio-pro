package com.mcardoso.srvcondominiopro.modules.areas;

import com.mcardoso.srvcondominiopro.modules.areas.dto.AreaResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.CreateAreaRequest;
import com.mcardoso.srvcondominiopro.modules.areas.dto.DisponibilidadeResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.HorarioDisponivelResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.UpdateAreaRequest;
import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.reservas.Reserva;
import com.mcardoso.srvcondominiopro.modules.reservas.ReservaRepository;
import com.mcardoso.srvcondominiopro.modules.reservas.StatusReserva;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class AreaComumService {

    private static final List<StatusReserva> RESERVAS_ATIVAS =
            List.of(StatusReserva.PENDENTE, StatusReserva.CONFIRMADA);

    private final AreaComumRepository areaComumRepository;
    private final CondominioRepository condominioRepository;
    private final ReservaRepository reservaRepository;

    public AreaComumService(
            AreaComumRepository areaComumRepository,
            CondominioRepository condominioRepository,
            ReservaRepository reservaRepository
    ) {
        this.areaComumRepository = areaComumRepository;
        this.condominioRepository = condominioRepository;
        this.reservaRepository = reservaRepository;
    }

    public List<AreaResponse> listar(Long condominioId, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        return areaComumRepository.findByCondominioIdOrderByNomeAsc(condominioId).stream()
                .map(AreaResponse::from)
                .toList();
    }

    @Transactional
    public AreaResponse criar(Long condominioId, CreateAreaRequest request, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        validarHorarios(request.horarioInicio(), request.horarioFim());

        Condominio condominio = condominioRepository.getReferenceById(condominioId);

        AreaComum area = new AreaComum();
        area.setNome(request.nome());
        area.setDescricao(request.descricao());
        area.setCapacidade(request.capacidade());
        area.setTaxa(request.taxa() != null ? request.taxa() : BigDecimal.ZERO);
        area.setRequerAprovacao(Boolean.TRUE.equals(request.requerAprovacao()));
        area.setFotoUrl(request.fotoUrl());
        area.setHorarioInicio(request.horarioInicio());
        area.setHorarioFim(request.horarioFim());
        area.setAntecedenciaMin(request.antecedenciaMin() != null ? request.antecedenciaMin() : 1);
        area.setAntecedenciaMax(request.antecedenciaMax() != null ? request.antecedenciaMax() : 30);
        area.setLimiteMensal(request.limiteMensal());
        area.setCondominio(condominio);
        areaComumRepository.save(area);

        return AreaResponse.from(area);
    }

    public AreaResponse buscar(Long id, Usuario usuarioLogado) {
        AreaComum area = buscarPorId(id);
        validarAcessoCondominio(area.getCondominio().getId(), usuarioLogado);
        return AreaResponse.from(area);
    }

    @Transactional
    public AreaResponse atualizar(Long id, UpdateAreaRequest request, Usuario usuarioLogado) {
        AreaComum area = buscarPorId(id);
        validarAcessoCondominio(area.getCondominio().getId(), usuarioLogado);
        validarHorarios(request.horarioInicio(), request.horarioFim());

        area.setNome(request.nome());
        area.setDescricao(request.descricao());
        area.setCapacidade(request.capacidade());
        area.setTaxa(request.taxa() != null ? request.taxa() : BigDecimal.ZERO);
        area.setRequerAprovacao(Boolean.TRUE.equals(request.requerAprovacao()));
        area.setAtiva(request.ativa() == null || request.ativa());
        area.setFotoUrl(request.fotoUrl());
        area.setHorarioInicio(request.horarioInicio());
        area.setHorarioFim(request.horarioFim());
        area.setAntecedenciaMin(request.antecedenciaMin() != null ? request.antecedenciaMin() : 1);
        area.setAntecedenciaMax(request.antecedenciaMax() != null ? request.antecedenciaMax() : 30);
        area.setLimiteMensal(request.limiteMensal());

        return AreaResponse.from(area);
    }

    @Transactional
    public void deletar(Long id, Usuario usuarioLogado) {
        AreaComum area = buscarPorId(id);
        validarAcessoCondominio(area.getCondominio().getId(), usuarioLogado);
        if (reservaRepository.existsByAreaIdAndStatusIn(id, RESERVAS_ATIVAS)) {
            throw new ConflictException("Não é possível excluir uma área com reservas ativas");
        }
        areaComumRepository.delete(area);
    }

    public DisponibilidadeResponse disponibilidade(Long id, LocalDate data, Usuario usuarioLogado) {
        AreaComum area = buscarPorId(id);
        validarAcessoCondominio(area.getCondominio().getId(), usuarioLogado);

        LocalTime inicio = LocalTime.parse(area.getHorarioInicio());
        LocalTime fim = LocalTime.parse(area.getHorarioFim());

        List<Reserva> reservasDoDia = reservaRepository.findByAreaIdAndDataAndStatusIn(
                area.getId(), data, RESERVAS_ATIVAS);

        // Gera blocos de 1h dentro do horário de funcionamento da área, descartando os que
        // colidem com uma reserva PENDENTE ou CONFIRMADA para a mesma data.
        List<HorarioDisponivelResponse> horarios = new ArrayList<>();
        LocalTime cursor = inicio;
        while (cursor.plusHours(1).compareTo(fim) <= 0) {
            LocalTime proximo = cursor.plusHours(1);
            if (!blocoReservado(cursor, proximo, reservasDoDia)) {
                horarios.add(new HorarioDisponivelResponse(cursor.toString(), proximo.toString()));
            }
            cursor = proximo;
        }

        return new DisponibilidadeResponse(area.getId(), data, horarios);
    }

    private AreaComum buscarPorId(Long id) {
        return areaComumRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Área comum não encontrada"));
    }

    private static boolean blocoReservado(LocalTime inicio, LocalTime fim, List<Reserva> reservas) {
        for (Reserva reserva : reservas) {
            LocalTime rInicio = LocalTime.parse(reserva.getHoraInicio());
            LocalTime rFim = LocalTime.parse(reserva.getHoraFim());
            if (inicio.isBefore(rFim) && rInicio.isBefore(fim)) {
                return true;
            }
        }
        return false;
    }

    private void validarHorarios(String horarioInicio, String horarioFim) {
        try {
            LocalTime inicio = LocalTime.parse(horarioInicio);
            LocalTime fim = LocalTime.parse(horarioFim);
            if (!inicio.isBefore(fim)) {
                throw new AppException("Horário de início deve ser antes do horário de fim", HttpStatus.BAD_REQUEST);
            }
        } catch (DateTimeException ex) {
            throw new AppException("Horário inválido, use o formato HH:mm", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarAcessoCondominio(Long condominioId, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }
}
