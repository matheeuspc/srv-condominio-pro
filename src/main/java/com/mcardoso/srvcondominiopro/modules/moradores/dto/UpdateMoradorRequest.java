package com.mcardoso.srvcondominiopro.modules.moradores.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateMoradorRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        String telefone
) {
}
