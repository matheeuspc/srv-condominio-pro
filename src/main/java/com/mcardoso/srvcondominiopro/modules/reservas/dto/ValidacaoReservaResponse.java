package com.mcardoso.srvcondominiopro.modules.reservas.dto;

import com.mcardoso.srvcondominiopro.modules.reservas.StatusReserva;

import java.math.BigDecimal;

public record ValidacaoReservaResponse(
        boolean valido,
        String motivo,
        BigDecimal taxa,
        boolean requerPagamento,
        boolean requerAprovacao,
        StatusReserva statusInicial
) {
    public static ValidacaoReservaResponse invalido(String motivo) {
        return new ValidacaoReservaResponse(false, motivo, null, false, false, null);
    }

    public static ValidacaoReservaResponse valido(
            BigDecimal taxa, boolean requerPagamento, boolean requerAprovacao, StatusReserva statusInicial) {
        return new ValidacaoReservaResponse(true, null, taxa, requerPagamento, requerAprovacao, statusInicial);
    }
}
