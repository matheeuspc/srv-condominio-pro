package com.mcardoso.srvcondominiopro.modules.pagamentos.dto;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioFinanceiroResponse(
        Long condominioId,
        BigDecimal totalRecebido,
        BigDecimal totalAguardando,
        int totalPagamentos,
        long pagamentosConfirmados,
        List<PagamentoResponse> pagamentos
) {
}
