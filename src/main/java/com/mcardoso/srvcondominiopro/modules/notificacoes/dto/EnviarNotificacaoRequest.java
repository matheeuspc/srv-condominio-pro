package com.mcardoso.srvcondominiopro.modules.notificacoes.dto;

import com.mcardoso.srvcondominiopro.modules.notificacoes.TipoNotificacao;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Envio manual pelo síndico (CONTEXT 6.10). Alvo:
//   - usuarioIds preenchido  -> exatamente esses usuários (têm de ser do condomínio do síndico)
//   - senão destinatario     -> todos os moradores ativos daquele papel
//   - senão                  -> todos os moradores ativos do condomínio
// tipo nulo => segue a preferência de cada usuário.
public record EnviarNotificacaoRequest(
        List<Long> usuarioIds,

        Role destinatario,

        TipoNotificacao tipo,

        @Size(max = 255, message = "Assunto deve ter no máximo 255 caracteres")
        String assunto,

        @NotBlank(message = "Conteúdo é obrigatório")
        String conteudo
) {
}
