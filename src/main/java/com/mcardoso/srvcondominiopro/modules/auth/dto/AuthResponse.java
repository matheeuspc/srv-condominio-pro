package com.mcardoso.srvcondominiopro.modules.auth.dto;

public record AuthResponse(
        String token,
        String tipo,
        UsuarioResponse usuario
) {
    public static AuthResponse of(String token, UsuarioResponse usuario) {
        return new AuthResponse(token, "Bearer", usuario);
    }
}
