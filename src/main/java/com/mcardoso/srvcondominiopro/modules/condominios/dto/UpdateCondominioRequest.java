package com.mcardoso.srvcondominiopro.modules.condominios.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCondominioRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Endereço é obrigatório")
        String endereco,

        String telefone,

        String logoUrl,

        boolean notificaEmail,

        boolean notificaWhatsapp
) {
}
