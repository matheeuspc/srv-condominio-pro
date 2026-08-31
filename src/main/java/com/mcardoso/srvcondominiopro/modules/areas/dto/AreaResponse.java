package com.mcardoso.srvcondominiopro.modules.areas.dto;

import com.mcardoso.srvcondominiopro.modules.areas.AreaComum;

import java.math.BigDecimal;

public record AreaResponse(
        Long id,
        String nome,
        String descricao,
        Integer capacidade,
        BigDecimal taxa,
        boolean requerAprovacao,
        boolean ativa,
        String fotoUrl,
        String horarioInicio,
        String horarioFim,
        Integer antecedenciaMin,
        Integer antecedenciaMax,
        Integer limiteMensal,
        Long condominioId
) {
    public static AreaResponse from(AreaComum area) {
        return new AreaResponse(
                area.getId(),
                area.getNome(),
                area.getDescricao(),
                area.getCapacidade(),
                area.getTaxa(),
                area.isRequerAprovacao(),
                area.isAtiva(),
                area.getFotoUrl(),
                area.getHorarioInicio(),
                area.getHorarioFim(),
                area.getAntecedenciaMin(),
                area.getAntecedenciaMax(),
                area.getLimiteMensal(),
                area.getCondominio().getId()
        );
    }
}
