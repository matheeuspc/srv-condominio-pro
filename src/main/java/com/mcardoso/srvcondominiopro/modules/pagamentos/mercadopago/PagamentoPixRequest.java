package com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago;

import java.math.BigDecimal;

// Comando interno para gerar uma cobrança Pix no Mercado Pago.
public record PagamentoPixRequest(
        BigDecimal valor,
        String descricao,
        String pagadorEmail,
        String pagadorNome,
        String referenciaExterna
) {
}
