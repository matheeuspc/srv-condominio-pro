package com.mcardoso.srvcondominiopro.modules.relatorios;

import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioComunicacaoResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioOcupacaoResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioPagamentosResponse;
import com.mcardoso.srvcondominiopro.modules.relatorios.dto.RelatorioReservasResponse;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

// Todos os endpoints são SINDICO — cobertos pelo matcher "/api/v1/condominios/**" no SecurityConfig.
// `inicio`/`fim` (ISO yyyy-MM-dd) são opcionais; default = primeiro dia de 12 meses atrás até hoje.
@RestController
@RequestMapping("/api/v1/condominios/{condominioId}/relatorios")
public class RelatorioController {

    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/reservas")
    public ResponseEntity<RelatorioReservasResponse> reservas(
            @PathVariable Long condominioId,
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(relatorioService.reservas(condominioId, inicio, fim, usuarioLogado));
    }

    @GetMapping("/pagamentos")
    public ResponseEntity<RelatorioPagamentosResponse> pagamentos(
            @PathVariable Long condominioId,
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(relatorioService.pagamentos(condominioId, inicio, fim, usuarioLogado));
    }

    @GetMapping("/ocupacao")
    public ResponseEntity<RelatorioOcupacaoResponse> ocupacao(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(relatorioService.ocupacao(condominioId, usuarioLogado));
    }

    @GetMapping("/comunicacao")
    public ResponseEntity<RelatorioComunicacaoResponse> comunicacao(
            @PathVariable Long condominioId,
            @RequestParam(value = "inicio", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(value = "fim", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(relatorioService.comunicacao(condominioId, inicio, fim, usuarioLogado));
    }
}
