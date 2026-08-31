package com.mcardoso.srvcondominiopro.modules.pagamentos;

import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.CriarCobrancaRequest;
import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.MercadoPagoWebhookRequest;
import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.PagamentoResponse;
import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.RelatorioFinanceiroResponse;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PagamentoController {

    private final PagamentoService pagamentoService;

    public PagamentoController(PagamentoService pagamentoService) {
        this.pagamentoService = pagamentoService;
    }

    @PostMapping("/api/v1/pagamentos/criar-cobranca")
    public ResponseEntity<PagamentoResponse> criarCobranca(
            @Valid @RequestBody CriarCobrancaRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        PagamentoResponse response = pagamentoService.criarCobranca(request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/pagamentos/{id}/status")
    public ResponseEntity<PagamentoResponse> status(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(pagamentoService.consultarStatus(id, usuarioLogado));
    }

    @GetMapping("/api/v1/reservas/{reservaId}/pagamento")
    public ResponseEntity<PagamentoResponse> porReserva(
            @PathVariable Long reservaId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(pagamentoService.buscarPorReserva(reservaId, usuarioLogado));
    }

    @GetMapping("/api/v1/condominios/{condominioId}/pagamentos")
    public ResponseEntity<RelatorioFinanceiroResponse> relatorio(
            @PathVariable Long condominioId,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(pagamentoService.relatorio(condominioId, usuarioLogado));
    }

    // Público: chamado pelo Mercado Pago. Autenticidade vem da assinatura (x-signature), não de JWT.
    // Sempre responde 200 quando a assinatura confere, para o MP não ficar reenviando.
    @PostMapping("/api/v1/pagamentos/webhook/mercadopago")
    public ResponseEntity<Void> webhookMercadoPago(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody(required = false) MercadoPagoWebhookRequest body,
            @RequestHeader(value = "x-signature", required = false) String signature,
            @RequestHeader(value = "x-request-id", required = false) String requestId
    ) {
        pagamentoService.processarWebhook(type, dataId, body, signature, requestId);
        return ResponseEntity.ok().build();
    }
}
