package com.mcardoso.srvcondominiopro.modules.faqs.dto;

import com.mcardoso.srvcondominiopro.modules.faqs.CategoriaFaq;
import com.mcardoso.srvcondominiopro.modules.faqs.Faq;

import java.time.LocalDateTime;

public record FaqResponse(
        Long id,
        String pergunta,
        String resposta,
        CategoriaFaq categoria,
        Integer ordem,
        boolean ativa,
        Long condominioId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FaqResponse from(Faq faq) {
        return new FaqResponse(
                faq.getId(),
                faq.getPergunta(),
                faq.getResposta(),
                faq.getCategoria(),
                faq.getOrdem(),
                faq.isAtiva(),
                faq.getCondominio().getId(),
                faq.getCreatedAt(),
                faq.getUpdatedAt()
        );
    }
}
