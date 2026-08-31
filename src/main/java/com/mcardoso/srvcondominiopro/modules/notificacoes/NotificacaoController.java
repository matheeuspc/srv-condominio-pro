package com.mcardoso.srvcondominiopro.modules.notificacoes;

import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.AtualizarPreferenciasRequest;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.EnviarNotificacaoRequest;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.EnvioResultadoResponse;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.NotificacaoResponse;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.PreferenciasResponse;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping("/api/v1/moradores/me/preferencias-notificacoes")
    public ResponseEntity<PreferenciasResponse> verPreferencias(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(notificacaoService.verPreferencias(usuarioLogado));
    }

    @PutMapping("/api/v1/moradores/me/preferencias-notificacoes")
    public ResponseEntity<PreferenciasResponse> atualizarPreferencias(
            @Valid @RequestBody AtualizarPreferenciasRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(notificacaoService.atualizarPreferencias(usuarioLogado, request));
    }

    @GetMapping("/api/v1/condominios/{condominioId}/notificacoes/log")
    public ResponseEntity<List<NotificacaoResponse>> log(
            @PathVariable Long condominioId,
            @RequestParam(value = "tipo", required = false) TipoNotificacao tipo,
            @RequestParam(value = "status", required = false) StatusNotificacao status,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(notificacaoService.log(condominioId, usuarioLogado, tipo, status));
    }

    @PostMapping("/api/v1/notificacoes/enviar")
    public ResponseEntity<EnvioResultadoResponse> enviar(
            @Valid @RequestBody EnviarNotificacaoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(notificacaoService.enviarManual(request, usuarioLogado));
    }
}
