package com.mcardoso.srvcondominiopro.modules.unidades.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUnidadeRequest(
        @Size(max = 10, message = "Bloco deve ter no máximo 10 caracteres")
        String bloco,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
        String numero
) {
}
