package com.mcardoso.srvcondominiopro.modules.areas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateAreaRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String descricao,

        @Positive(message = "Capacidade deve ser positiva")
        Integer capacidade,

        @DecimalMin(value = "0.0", message = "Taxa não pode ser negativa")
        BigDecimal taxa,

        Boolean requerAprovacao,

        String fotoUrl,

        @NotBlank(message = "Horário de início é obrigatório")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de início deve estar no formato HH:mm")
        String horarioInicio,

        @NotBlank(message = "Horário de fim é obrigatório")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Horário de fim deve estar no formato HH:mm")
        String horarioFim,

        @Positive(message = "Antecedência mínima deve ser positiva")
        Integer antecedenciaMin,

        @Positive(message = "Antecedência máxima deve ser positiva")
        Integer antecedenciaMax,

        @Positive(message = "Limite mensal deve ser positivo")
        Integer limiteMensal
) {
}
