package com.mcardoso.srvcondominiopro.modules.condominios.dto;

public record DashboardResponse(
        long totalUsuariosAtivos,
        long totalMoradores
) {
}
