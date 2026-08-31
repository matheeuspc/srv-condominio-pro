package com.mcardoso.srvcondominiopro.modules.moradores.dto;

import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMoradorRequest(
        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        String telefone,

        @NotNull(message = "Role é obrigatória")
        Role role
) {
}
