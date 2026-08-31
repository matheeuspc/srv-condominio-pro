package com.mcardoso.srvcondominiopro.modules.relatorios.dto;

import com.mcardoso.srvcondominiopro.modules.notificacoes.StatusNotificacao;
import com.mcardoso.srvcondominiopro.modules.notificacoes.TipoNotificacao;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;

import java.time.LocalDate;
import java.util.List;

public record RelatorioComunicacaoResponse(
        Long condominioId,
        LocalDate inicio,
        LocalDate fim,
        long avisosPublicados,
        List<AvisoEngajamento> avisos,
        List<NotificacaoAgregada> notificacoes
) {
    public record AvisoEngajamento(
            Long avisoId,
            String titulo,
            Role destinatario,
            long elegiveis,
            long leituras,
            double taxaLeitura
    ) {
    }

    public record NotificacaoAgregada(
            TipoNotificacao tipo,
            StatusNotificacao status,
            long quantidade
    ) {
    }
}
