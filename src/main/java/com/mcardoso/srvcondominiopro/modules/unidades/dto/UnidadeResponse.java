package com.mcardoso.srvcondominiopro.modules.unidades.dto;

import com.mcardoso.srvcondominiopro.modules.unidades.Unidade;

public record UnidadeResponse(
        Long id,
        String bloco,
        String numero,
        Long condominioId
) {
    public static UnidadeResponse from(Unidade unidade) {
        return new UnidadeResponse(
                unidade.getId(),
                unidade.getBloco(),
                unidade.getNumero(),
                unidade.getCondominio().getId()
        );
    }
}
