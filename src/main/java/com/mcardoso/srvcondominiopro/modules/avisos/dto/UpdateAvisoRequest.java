package com.mcardoso.srvcondominiopro.modules.avisos.dto;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import jakarta.validation.constraints.NotBlank;

public record UpdateAvisoRequest(
        @NotBlank(message = "Título é obrigatório")
        String titulo,

        @NotBlank(message = "Conteúdo é obrigatório")
        String conteudo,

        String anexoUrl,

        Boolean publicado,

        Role destinatario
) {
}
