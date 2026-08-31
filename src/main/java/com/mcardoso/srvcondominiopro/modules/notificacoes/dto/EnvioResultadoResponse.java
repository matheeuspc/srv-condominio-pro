package com.mcardoso.srvcondominiopro.modules.notificacoes.dto;

public record EnvioResultadoResponse(
        int destinatarios,
        int enviados,
        int falhas
) {
}
