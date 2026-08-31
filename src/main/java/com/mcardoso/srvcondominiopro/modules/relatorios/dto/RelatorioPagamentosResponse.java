package com.mcardoso.srvcondominiopro.modules.relatorios.dto;

import com.mcardoso.srvcondominiopro.modules.pagamentos.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RelatorioPagamentosResponse(
        Long condominioId,
        LocalDate inicio,
        LocalDate fim,
        Map<StatusPagamento, StatusItem> porStatus,
        BigDecimal totalRecebido,
        BigDecimal ticketMedio,
        Map<String, ValorMensal> porMes,
        List<PagamentosPorArea> porArea
) {
    public record StatusItem(long quantidade, BigDecimal valor) {
    }

    public record ValorMensal(long quantidade, BigDecimal recebido) {
    }

    public record PagamentosPorArea(Long areaId, String areaNome, long pagos, BigDecimal recebido) {
    }
}
