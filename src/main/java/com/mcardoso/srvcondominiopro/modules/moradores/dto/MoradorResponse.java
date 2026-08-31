package com.mcardoso.srvcondominiopro.modules.moradores.dto;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;

import java.util.List;

public record MoradorResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        Role role,
        boolean ativo,
        Long condominioId,
        String tokenConvite,
        List<VinculoResponse> unidades
) {
}
