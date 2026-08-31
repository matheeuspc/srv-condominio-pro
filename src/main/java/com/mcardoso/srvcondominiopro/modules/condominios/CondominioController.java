package com.mcardoso.srvcondominiopro.modules.condominios;

import com.mcardoso.srvcondominiopro.modules.condominios.dto.CondominioResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.dto.DashboardResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.dto.UpdateCondominioRequest;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/condominios")
public class CondominioController {

    private final CondominioService condominioService;

    public CondominioController(CondominioService condominioService) {
        this.condominioService = condominioService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CondominioResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(condominioService.buscar(id, usuarioLogado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CondominioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCondominioRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(condominioService.atualizar(id, request, usuarioLogado));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(condominioService.dashboard(id, usuarioLogado));
    }
}
