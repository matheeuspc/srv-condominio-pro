package com.mcardoso.srvcondominiopro.modules.notificacoes.dto;

import com.mcardoso.srvcondominiopro.modules.notificacoes.PreferenciaNotificacao;

public record PreferenciasResponse(
        Long usuarioId,
        boolean notificarEmail,
        boolean notificarWhatsapp
) {
    public static PreferenciasResponse from(PreferenciaNotificacao pref) {
        return new PreferenciasResponse(
                pref.getUsuario().getId(),
                pref.isNotificarEmail(),
                pref.isNotificarWhatsapp()
        );
    }

    public static PreferenciasResponse padrao(Long usuarioId) {
        return new PreferenciasResponse(usuarioId, true, false);
    }
}
