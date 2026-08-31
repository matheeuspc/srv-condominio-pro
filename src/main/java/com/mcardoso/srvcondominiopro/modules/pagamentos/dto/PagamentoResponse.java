package com.mcardoso.srvcondominiopro.modules.pagamentos.dto;

import com.mcardoso.srvcondominiopro.modules.pagamentos.Pagamento;
import com.mcardoso.srvcondominiopro.modules.pagamentos.StatusPagamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagamentoResponse(
        Long id,
        Long reservaId,
        Long condominioId,
        BigDecimal valor,
        StatusPagamento status,
        String metodoPagamento,
        String qrCode,
        String qrCodeBase64,
        String ticketUrl,
        String mpPaymentId,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    public static PagamentoResponse from(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getReserva().getId(),
                pagamento.getReserva().getArea().getCondominio().getId(),
                pagamento.getValor(),
                pagamento.getStatus(),
                pagamento.getMetodoPagamento(),
                pagamento.getMpQrCode(),
                pagamento.getMpQrCodeBase64(),
                pagamento.getMpTicketUrl(),
                pagamento.getMpPaymentId(),
                pagamento.getPaidAt(),
                pagamento.getCreatedAt()
        );
    }
}
