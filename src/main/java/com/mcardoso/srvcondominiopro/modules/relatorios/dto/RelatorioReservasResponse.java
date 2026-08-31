package com.mcardoso.srvcondominiopro.modules.relatorios.dto;

import com.mcardoso.srvcondominiopro.modules.reservas.StatusReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record RelatorioReservasResponse(
        Long condominioId,
        LocalDate inicio,
        LocalDate fim,
        long total,
        Map<StatusReserva, Long> porStatus,
        Map<String, Long> porMes,
        List<ReservasPorArea> porArea
) {
    public record ReservasPorArea(
            Long areaId,
            String areaNome,
            long total,
            long confirmadas,
            BigDecimal taxaArrecadada
    ) {
    }
}
