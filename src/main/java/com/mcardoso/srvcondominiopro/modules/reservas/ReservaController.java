package com.mcardoso.srvcondominiopro.modules.reservas;

import com.mcardoso.srvcondominiopro.modules.reservas.dto.CreateReservaRequest;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.RejeitarReservaRequest;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.ReservaResponse;
import com.mcardoso.srvcondominiopro.modules.reservas.dto.ValidacaoReservaResponse;
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
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping("/api/v1/reservas")
    public ResponseEntity<ReservaResponse> criar(
            @Valid @RequestBody CreateReservaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        ReservaResponse response = reservaService.criar(request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/api/v1/reservas/validar")
    public ResponseEntity<ValidacaoReservaResponse> validar(
            @Valid @RequestBody CreateReservaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.validar(request, usuarioLogado));
    }

    @GetMapping("/api/v1/reservas/{id}")
    public ResponseEntity<ReservaResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.buscar(id, usuarioLogado));
    }

    @DeleteMapping("/api/v1/reservas/{id}")
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.cancelar(id, usuarioLogado));
    }

    @GetMapping("/api/v1/moradores/me/reservas")
    public ResponseEntity<List<ReservaResponse>> minhasReservas(
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.minhasReservas(usuarioLogado));
    }

    @GetMapping("/api/v1/areas/{areaId}/reservas")
    public ResponseEntity<List<ReservaResponse>> listarPorArea(
            @PathVariable Long areaId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.listarPorArea(areaId, usuarioLogado));
    }

    @GetMapping("/api/v1/condominios/{condominioId}/reservas")
    public ResponseEntity<List<ReservaResponse>> listarPorCondominio(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.listarPorCondominio(condominioId, usuarioLogado));
    }

    @PutMapping("/api/v1/reservas/{id}/aprovar")
    public ResponseEntity<ReservaResponse> aprovar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.aprovar(id, usuarioLogado));
    }

    @PutMapping("/api/v1/reservas/{id}/rejeitar")
    public ResponseEntity<ReservaResponse> rejeitar(
            @PathVariable Long id,
            @RequestBody(required = false) RejeitarReservaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(reservaService.rejeitar(id, request, usuarioLogado));
    }
}
