package com.mcardoso.srvcondominiopro.modules.faqs;

import com.mcardoso.srvcondominiopro.modules.faqs.dto.CreateFaqRequest;
import com.mcardoso.srvcondominiopro.modules.faqs.dto.FaqResponse;
import com.mcardoso.srvcondominiopro.modules.faqs.dto.UpdateFaqRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FaqController {

    private final FaqService faqService;

    public FaqController(FaqService faqService) {
        this.faqService = faqService;
    }

    @GetMapping("/api/v1/condominios/{condominioId}/faqs")
    public ResponseEntity<List<FaqResponse>> listar(
            @PathVariable Long condominioId,
            @RequestParam(value = "categoria", required = false) CategoriaFaq categoria,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(faqService.listar(condominioId, categoria, usuarioLogado));
    }

    @GetMapping("/api/v1/condominios/{condominioId}/faqs/search")
    public ResponseEntity<List<FaqResponse>> buscar(
            @PathVariable Long condominioId,
            @RequestParam("q") String q,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(faqService.buscar(condominioId, q, usuarioLogado));
    }

    @PostMapping("/api/v1/condominios/{condominioId}/faqs")
    public ResponseEntity<FaqResponse> criar(
            @PathVariable Long condominioId,
            @Valid @RequestBody CreateFaqRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        FaqResponse response = faqService.criar(condominioId, request, usuarioLogado);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/faqs/{id}")
    public ResponseEntity<FaqResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(faqService.buscarPorId(id, usuarioLogado));
    }

    @PutMapping("/api/v1/faqs/{id}")
    public ResponseEntity<FaqResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFaqRequest request,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        return ResponseEntity.ok(faqService.atualizar(id, request, usuarioLogado));
    }

    @DeleteMapping("/api/v1/faqs/{id}")
    public ResponseEntity<Void> deletar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuarioLogado
    ) {
        faqService.deletar(id, usuarioLogado);
        return ResponseEntity.noContent().build();
    }
}
