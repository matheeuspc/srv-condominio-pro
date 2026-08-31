package com.mcardoso.srvcondominiopro.modules.areas;

import com.mcardoso.srvcondominiopro.modules.areas.dto.AreaResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.CreateAreaRequest;
import com.mcardoso.srvcondominiopro.modules.areas.dto.DisponibilidadeResponse;
import com.mcardoso.srvcondominiopro.modules.areas.dto.UpdateAreaRequest;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class AreaComumController {

    private final AreaComumService areaComumService;

    public AreaComumController(AreaComumService areaComumService) {
        this.areaComumService = areaComumService;
    }

    @GetMapping("/api/v1/condominios/{condominioId}/areas")
    public ResponseEntity<List<AreaResponse>> listar(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(areaComumService.listar(condominioId, usuarioLogado));
    }

    @PostMapping("/api/v1/condominios/{condominioId}/areas")
    public ResponseEntity<AreaResponse> criar(
            @PathVariable Long condominioId,
            @Valid @RequestBody CreateAreaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        AreaResponse response = areaComumService.criar(condominioId, request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/areas/{id}")
    public ResponseEntity<AreaResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(areaComumService.buscar(id, usuarioLogado));
    }

    @PutMapping("/api/v1/areas/{id}")
    public ResponseEntity<AreaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAreaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(areaComumService.atualizar(id, request, usuarioLogado));
    }

    @DeleteMapping("/api/v1/areas/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        areaComumService.deletar(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/areas/{id}/disponibilidade")
    public ResponseEntity<DisponibilidadeResponse> disponibilidade(
            @PathVariable Long id,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(areaComumService.disponibilidade(id, data, usuarioLogado));
    }
}
