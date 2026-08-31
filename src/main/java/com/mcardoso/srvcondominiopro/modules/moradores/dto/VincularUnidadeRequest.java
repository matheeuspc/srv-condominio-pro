package com.mcardoso.srvcondominiopro.modules.moradores.dto;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import jakarta.validation.constraints.NotNull;

public record VincularUnidadeRequest(
        @NotNull(message = "Unidade é obrigatória")
        Long unidadeId,

        @NotNull(message = "Tipo de vínculo é obrigatório")
        Role tipo
) {
}
