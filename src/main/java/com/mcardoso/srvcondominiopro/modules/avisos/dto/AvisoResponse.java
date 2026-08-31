package com.mcardoso.srvcondominiopro.modules.avisos.dto;

import com.mcardoso.srvcondominiopro.modules.avisos.Aviso;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;

import java.time.LocalDateTime;

public record AvisoResponse(
        Long id,
        String titulo,
        String conteudo,
        String anexoUrl,
        boolean publicado,
        Role destinatario,
        Long condominioId,
        Long autorId,
        String autorNome,
        boolean lido,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AvisoResponse from(Aviso aviso, boolean lido) {
        return new AvisoResponse(
                aviso.getId(),
                aviso.getTitulo(),
                aviso.getConteudo(),
                aviso.getAnexoUrl(),
                aviso.isPublicado(),
                aviso.getDestinatario(),
                aviso.getCondominio().getId(),
                aviso.getAutor().getId(),
                aviso.getAutor().getNome(),
                lido,
                aviso.getCreatedAt(),
                aviso.getUpdatedAt()
        );
    }

    public static AvisoResponse from(Aviso aviso) {
        return from(aviso, false);
    }
}
