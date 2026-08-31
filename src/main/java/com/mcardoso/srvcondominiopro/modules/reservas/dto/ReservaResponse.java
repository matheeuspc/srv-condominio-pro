package com.mcardoso.srvcondominiopro.modules.reservas.dto;

import com.mcardoso.srvcondominiopro.modules.reservas.Reserva;
import com.mcardoso.srvcondominiopro.modules.reservas.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long areaId,
        String areaNome,
        Long condominioId,
        Long usuarioId,
        String usuarioNome,
        Long unidadeId,
        String unidadeBloco,
        String unidadeNumero,
        LocalDate data,
        String horaInicio,
        String horaFim,
        StatusReserva status,
        String observacao,
        BigDecimal taxa,
        boolean requerAprovacao,
        LocalDateTime createdAt
) {
    public static ReservaResponse from(Reserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getArea().getId(),
                reserva.getArea().getNome(),
                reserva.getArea().getCondominio().getId(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNome(),
                reserva.getUnidade().getId(),
                reserva.getUnidade().getBloco(),
                reserva.getUnidade().getNumero(),
                reserva.getData(),
                reserva.getHoraInicio(),
                reserva.getHoraFim(),
                reserva.getStatus(),
                reserva.getObservacao(),
                reserva.getArea().getTaxa(),
                reserva.getArea().isRequerAprovacao(),
                reserva.getCreatedAt()
        );
    }
}
