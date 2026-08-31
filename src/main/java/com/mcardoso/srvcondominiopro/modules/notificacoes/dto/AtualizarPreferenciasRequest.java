package com.mcardoso.srvcondominiopro.modules.notificacoes.dto;

// Campos nulos = mantém o valor atual.
public record AtualizarPreferenciasRequest(
        Boolean notificarEmail,
        Boolean notificarWhatsapp
) {
}
