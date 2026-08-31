package com.mcardoso.srvcondominiopro.modules.avisos;

import com.mcardoso.srvcondominiopro.modules.avisos.dto.AvisoResponse;
import com.mcardoso.srvcondominiopro.modules.avisos.dto.CreateAvisoRequest;
import com.mcardoso.srvcondominiopro.modules.avisos.dto.UpdateAvisoRequest;
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
public class AvisoController {

    private final AvisoService avisoService;

    public AvisoController(AvisoService avisoService) {
        this.avisoService = avisoService;
    }

    @PostMapping("/api/v1/condominios/{condominioId}/avisos")
    public ResponseEntity<AvisoResponse> criar(
            @PathVariable Long condominioId,
            @Valid @RequestBody CreateAvisoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        AvisoResponse response = avisoService.criar(condominioId, request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/condominios/{condominioId}/avisos")
    public ResponseEntity<List<AvisoResponse>> listar(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(avisoService.listar(condominioId, usuarioLogado));
    }

    @GetMapping("/api/v1/avisos/nao-lidos")
    public ResponseEntity<List<AvisoResponse>> naoLidos(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(avisoService.naoLidos(usuarioLogado));
    }

    @GetMapping("/api/v1/avisos/{id}")
    public ResponseEntity<AvisoResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(avisoService.buscar(id, usuarioLogado));
    }

    @PutMapping("/api/v1/avisos/{id}")
    public ResponseEntity<AvisoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAvisoRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(avisoService.atualizar(id, request, usuarioLogado));
    }

    @DeleteMapping("/api/v1/avisos/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        avisoService.deletar(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/avisos/{id}/marcar-lido")
    public ResponseEntity<Void> marcarLido(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        avisoService.marcarLido(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }
}
