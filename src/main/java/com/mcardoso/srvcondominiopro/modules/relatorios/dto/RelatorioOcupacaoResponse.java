package com.mcardoso.srvcondominiopro.modules.relatorios.dto;

// Snapshot atual (sem período): ocupação de unidades e composição do condomínio.
public record RelatorioOcupacaoResponse(
        Long condominioId,
        long unidadesTotal,
        long unidadesOcupadas,
        long unidadesVazias,
        long moradoresAtivos,
        long proprietariosAtivos,
        long inquilinosAtivos,
        long areasTotal,
        long areasAtivas
) {
}
