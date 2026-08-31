package com.mcardoso.srvcondominiopro.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Token é obrigatório")
        String token
) {
}
