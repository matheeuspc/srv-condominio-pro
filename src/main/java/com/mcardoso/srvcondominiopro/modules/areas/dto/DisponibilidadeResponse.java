package com.mcardoso.srvcondominiopro.modules.areas.dto;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadeResponse(
        Long areaId,
        LocalDate data,
        List<HorarioDisponivelResponse> horarios
) {
}
