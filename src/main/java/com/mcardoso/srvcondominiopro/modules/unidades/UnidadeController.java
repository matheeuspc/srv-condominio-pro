package com.mcardoso.srvcondominiopro.modules.unidades;

import com.mcardoso.srvcondominiopro.modules.unidades.dto.CreateUnidadeRequest;
import com.mcardoso.srvcondominiopro.modules.unidades.dto.UnidadeResponse;
import com.mcardoso.srvcondominiopro.modules.unidades.dto.UpdateUnidadeRequest;
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
public class UnidadeController {

    private final UnidadeService unidadeService;

    public UnidadeController(UnidadeService unidadeService) {
        this.unidadeService = unidadeService;
    }

    @GetMapping("/api/v1/condominios/{condominioId}/unidades")
    public ResponseEntity<List<UnidadeResponse>> listar(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(unidadeService.listar(condominioId, usuarioLogado));
    }

    @PostMapping("/api/v1/condominios/{condominioId}/unidades")
    public ResponseEntity<UnidadeResponse> criar(
            @PathVariable Long condominioId,
            @Valid @RequestBody CreateUnidadeRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        UnidadeResponse response = unidadeService.criar(condominioId, request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/unidades/{id}")
    public ResponseEntity<UnidadeResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(unidadeService.buscar(id, usuarioLogado));
    }

    @PutMapping("/api/v1/unidades/{id}")
    public ResponseEntity<UnidadeResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUnidadeRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(unidadeService.atualizar(id, request, usuarioLogado));
    }

    @DeleteMapping("/api/v1/unidades/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        unidadeService.deletar(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }
}
