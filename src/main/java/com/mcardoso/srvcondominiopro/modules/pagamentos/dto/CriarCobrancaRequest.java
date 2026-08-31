package com.mcardoso.srvcondominiopro.modules.pagamentos.dto;

import jakarta.validation.constraints.NotNull;

public record CriarCobrancaRequest(
        @NotNull(message = "reservaId é obrigatório")
        Long reservaId
) {
}
