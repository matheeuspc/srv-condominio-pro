package com.mcardoso.srvcondominiopro.modules.faqs.dto;

import com.mcardoso.srvcondominiopro.modules.faqs.CategoriaFaq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateFaqRequest(
        @NotBlank(message = "Pergunta é obrigatória")
        String pergunta,

        @NotBlank(message = "Resposta é obrigatória")
        String resposta,

        // default OUTROS quando ausente
        CategoriaFaq categoria,

        @PositiveOrZero(message = "Ordem não pode ser negativa")
        Integer ordem,

        // default true quando ausente
        Boolean ativa
) {
}
