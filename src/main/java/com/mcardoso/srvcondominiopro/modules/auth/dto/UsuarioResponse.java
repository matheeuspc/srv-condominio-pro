package com.mcardoso.srvcondominiopro.modules.auth.dto;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Role role,
        Long condominioId
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getCondominio().getId()
        );
    }
}
