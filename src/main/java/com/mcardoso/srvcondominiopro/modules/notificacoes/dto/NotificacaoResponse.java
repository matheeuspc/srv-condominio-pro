package com.mcardoso.srvcondominiopro.modules.notificacoes.dto;

import com.mcardoso.srvcondominiopro.modules.notificacoes.Notificacao;
import com.mcardoso.srvcondominiopro.modules.notificacoes.StatusNotificacao;
import com.mcardoso.srvcondominiopro.modules.notificacoes.TipoNotificacao;

import java.time.LocalDateTime;

public record NotificacaoResponse(
        Long id,
        Long usuarioId,
        String usuarioNome,
        TipoNotificacao tipo,
        StatusNotificacao status,
        String assunto,
        String conteudo,
        LocalDateTime enviadoEm,
        String erro,
        LocalDateTime createdAt
) {
    public static NotificacaoResponse from(Notificacao n) {
        return new NotificacaoResponse(
                n.getId(),
                n.getUsuario().getId(),
                n.getUsuario().getNome(),
                n.getTipo(),
                n.getStatus(),
                n.getAssunto(),
                n.getConteudo(),
                n.getEnviadoEm(),
                n.getErro(),
                n.getCreatedAt()
        );
    }
}
