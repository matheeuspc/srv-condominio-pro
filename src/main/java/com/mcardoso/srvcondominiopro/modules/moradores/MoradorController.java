package com.mcardoso.srvcondominiopro.modules.moradores;

import com.mcardoso.srvcondominiopro.modules.auth.dto.AuthResponse;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.AceitarConviteRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.CreateMoradorRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.MoradorResponse;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.UpdateMoradorRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.VincularUnidadeRequest;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MoradorController {

    private final MoradorService moradorService;

    public MoradorController(MoradorService moradorService) {
        this.moradorService = moradorService;
    }

    @GetMapping("/api/v1/condominios/{condominioId}/moradores")
    public ResponseEntity<List<MoradorResponse>> listar(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(moradorService.listar(condominioId, usuarioLogado));
    }

    @PostMapping("/api/v1/condominios/{condominioId}/moradores")
    public ResponseEntity<MoradorResponse> criar(
            @PathVariable Long condominioId,
            @Valid @RequestBody CreateMoradorRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        MoradorResponse response = moradorService.criar(condominioId, request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/moradores/{id}")
    public ResponseEntity<MoradorResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(moradorService.buscar(id, usuarioLogado));
    }

    @PutMapping("/api/v1/moradores/{id}")
    public ResponseEntity<MoradorResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMoradorRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(moradorService.atualizar(id, request, usuarioLogado));
    }

    @DeleteMapping("/api/v1/moradores/{id}")
    public ResponseEntity<Void> desativar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        moradorService.desativar(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/moradores/{id}/vincular-unidade")
    public ResponseEntity<MoradorResponse> vincularUnidade(
            @PathVariable Long id,
            @Valid @RequestBody VincularUnidadeRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        MoradorResponse response = moradorService.vincularUnidade(id, request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/moradores/me")
    public ResponseEntity<MoradorResponse> me(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(moradorService.me(usuarioLogado));
    }

    @PostMapping("/api/v1/moradores/convite/{token}")
    public ResponseEntity<AuthResponse> aceitarConvite(
            @PathVariable String token,
            @Valid @RequestBody AceitarConviteRequest request
    ) {
        return ResponseEntity.ok(moradorService.aceitarConvite(token, request));
    }
}
