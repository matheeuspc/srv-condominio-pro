package com.mcardoso.srvcondominiopro.modules.reservas.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateReservaRequest(
        @NotNull(message = "areaId é obrigatório")
        Long areaId,

        @NotNull(message = "unidadeId é obrigatório")
        Long unidadeId,

        @NotNull(message = "Data é obrigatória")
        @FutureOrPresent(message = "Data não pode estar no passado")
        LocalDate data,

        @NotBlank(message = "Hora de início é obrigatória")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Hora de início deve estar no formato HH:mm")
        String horaInicio,

        @NotBlank(message = "Hora de fim é obrigatória")
        @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "Hora de fim deve estar no formato HH:mm")
        String horaFim,

        String observacao
) {
}
