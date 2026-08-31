package com.mcardoso.srvcondominiopro.modules.condominios.dto;

import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;

public record CondominioResponse(
        Long id,
        String nome,
        String cnpj,
        String endereco,
        String telefone,
        String logoUrl,
        String plano,
        boolean ativo,
        boolean notificaEmail,
        boolean notificaWhatsapp
) {
    public static CondominioResponse from(Condominio condominio) {
        return new CondominioResponse(
                condominio.getId(),
                condominio.getNome(),
                condominio.getCnpj(),
                condominio.getEndereco(),
                condominio.getTelefone(),
                condominio.getLogoUrl(),
                condominio.getPlano(),
                condominio.isAtivo(),
                condominio.isNotificaEmail(),
                condominio.isNotificaWhatsapp()
        );
    }
}
