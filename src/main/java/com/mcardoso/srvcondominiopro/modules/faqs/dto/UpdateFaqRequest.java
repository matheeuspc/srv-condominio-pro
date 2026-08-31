package com.mcardoso.srvcondominiopro.modules.faqs.dto;

import com.mcardoso.srvcondominiopro.modules.faqs.CategoriaFaq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateFaqRequest(
        @NotBlank(message = "Pergunta é obrigatória")
        String pergunta,

        @NotBlank(message = "Resposta é obrigatória")
        String resposta,

        CategoriaFaq categoria,

        @PositiveOrZero(message = "Ordem não pode ser negativa")
        Integer ordem,

        Boolean ativa
) {
}
