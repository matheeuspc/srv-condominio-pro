package com.mcardoso.srvcondominiopro.modules.moradores.dto;

import com.mcardoso.srvcondominiopro.modules.moradores.MoradorUnidade;
import com.mcardoso.srvcondominiopro.modules.moradores.StatusMorador;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;

public record VinculoResponse(
        Long unidadeId,
        String bloco,
        String numero,
        Role tipo,
        StatusMorador status
) {
    public static VinculoResponse from(MoradorUnidade vinculo) {
        return new VinculoResponse(
                vinculo.getUnidade().getId(),
                vinculo.getUnidade().getBloco(),
                vinculo.getUnidade().getNumero(),
                vinculo.getTipo(),
                vinculo.getStatus()
        );
    }
}
