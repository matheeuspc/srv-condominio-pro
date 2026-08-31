package com.mcardoso.srvcondominiopro.modules.avisos.dto;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import jakarta.validation.constraints.NotBlank;

public record CreateAvisoRequest(
        @NotBlank(message = "Título é obrigatório")
        String titulo,

        @NotBlank(message = "Conteúdo é obrigatório")
        String conteudo,

        String anexoUrl,

        // default true quando ausente
        Boolean publicado,

        // null = todos os moradores; PROPRIETARIO / INQUILINO = segmento
        Role destinatario
) {
}
