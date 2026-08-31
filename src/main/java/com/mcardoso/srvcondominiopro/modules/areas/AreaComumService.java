package com.mcardoso.srvcondominiopro.modules.areas;

import com.mcardoso.srvcondominiopro.modules.areas.dto.AreaResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.CreateAreaRequest;
import com.mcardoso.srvcondominiopro.modules.areas.dto.DisponibilidadeResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.HorarioDisponivelResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.UpdateAreaRequest;
import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
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

    private final AreaComumRepository areaComumRepository;
    private final CondominioRepository condominioRepository;

    public AreaComumService(AreaComumRepository areaComumRepository, CondominioRepository condominioRepository) {
        this.areaComumRepository = areaComumRepository;
        this.condominioRepository = condominioRepository;
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
        // Bloquear exclusão com reservas ativas fica para o módulo de Reservas (ainda não existe).
        areaComumRepository.delete(area);
    }

    public DisponibilidadeResponse disponibilidade(Long id, LocalDate data, Usuario usuarioLogado) {
        AreaComum area = buscarPorId(id);
        validarAcessoCondominio(area.getCondominio().getId(), usuarioLogado);

        LocalTime inicio = LocalTime.parse(area.getHorarioInicio());
        LocalTime fim = LocalTime.parse(area.getHorarioFim());

        // Gera blocos de 1h dentro do horário de funcionamento da área. Ainda não descarta
        // horários já reservados, pois o módulo de Reservas (que guarda essa informação) não existe.
        List<HorarioDisponivelResponse> horarios = new ArrayList<>();
        LocalTime cursor = inicio;
        while (cursor.plusHours(1).compareTo(fim) <= 0) {
            LocalTime proximo = cursor.plusHours(1);
            horarios.add(new HorarioDisponivelResponse(cursor.toString(), proximo.toString()));
            cursor = proximo;
        }

        return new DisponibilidadeResponse(area.getId(), data, horarios);
    }

    private AreaComum buscarPorId(Long id) {
        return areaComumRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Área comum não encontrada"));
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
