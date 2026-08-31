package com.mcardoso.srvcondominiopro.modules.faqs;

import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.faqs.dto.CreateFaqRequest;
import com.mcardoso.srvcondominiopro.modules.faqs.dto.FaqResponse;
import com.mcardoso.srvcondominiopro.modules.faqs.dto.UpdateFaqRequest;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FaqService {

    private final FaqRepository faqRepository;
    private final CondominioRepository condominioRepository;

    public FaqService(FaqRepository faqRepository, CondominioRepository condominioRepository) {
        this.faqRepository = faqRepository;
        this.condominioRepository = condominioRepository;
    }

    public List<FaqResponse> listar(Long condominioId, CategoriaFaq categoria, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        boolean incluirInativas = usuarioLogado.getRole() == Role.SINDICO;

        List<Faq> faqs;
        if (categoria != null) {
            faqs = incluirInativas
                    ? faqRepository.findByCondominioIdAndCategoriaOrderByOrdemAscIdAsc(condominioId, categoria)
                    : faqRepository.findByCondominioIdAndAtivaTrueAndCategoriaOrderByOrdemAscIdAsc(condominioId, categoria);
        } else {
            faqs = incluirInativas
                    ? faqRepository.findByCondominioIdOrderByOrdemAscIdAsc(condominioId)
                    : faqRepository.findByCondominioIdAndAtivaTrueOrderByOrdemAscIdAsc(condominioId);
        }
        return faqs.stream().map(FaqResponse::from).toList();
    }

    public List<FaqResponse> buscar(Long condominioId, String termo, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        if (termo == null || termo.isBlank()) {
            throw new AppException("Parâmetro de busca 'q' é obrigatório", HttpStatus.BAD_REQUEST);
        }
        boolean incluirInativas = usuarioLogado.getRole() == Role.SINDICO;
        return faqRepository.buscar(condominioId, termo.trim()).stream()
                .filter(f -> incluirInativas || f.isAtiva())
                .map(FaqResponse::from)
                .toList();
    }

    @Transactional
    public FaqResponse criar(Long condominioId, CreateFaqRequest request, Usuario sindicoLogado) {
        validarAcessoCondominio(condominioId, sindicoLogado);

        Condominio condominio = condominioRepository.getReferenceById(condominioId);

        Faq faq = new Faq();
        faq.setPergunta(request.pergunta());
        faq.setResposta(request.resposta());
        faq.setCategoria(request.categoria() != null ? request.categoria() : CategoriaFaq.OUTROS);
        faq.setOrdem(request.ordem() != null ? request.ordem() : 0);
        faq.setAtiva(request.ativa() == null || request.ativa());
        faq.setCondominio(condominio);
        faqRepository.save(faq);

        return FaqResponse.from(faq);
    }

    public FaqResponse buscarPorId(Long id, Usuario usuarioLogado) {
        Faq faq = buscarEntidade(id);
        validarAcessoCondominio(faq.getCondominio().getId(), usuarioLogado);
        if (!faq.isAtiva() && usuarioLogado.getRole() != Role.SINDICO) {
            throw new NotFoundException("FAQ não encontrada");
        }
        return FaqResponse.from(faq);
    }

    @Transactional
    public FaqResponse atualizar(Long id, UpdateFaqRequest request, Usuario sindicoLogado) {
        Faq faq = buscarEntidade(id);
        validarAcessoCondominio(faq.getCondominio().getId(), sindicoLogado);

        faq.setPergunta(request.pergunta());
        faq.setResposta(request.resposta());
        faq.setCategoria(request.categoria() != null ? request.categoria() : CategoriaFaq.OUTROS);
        faq.setOrdem(request.ordem() != null ? request.ordem() : 0);
        faq.setAtiva(request.ativa() == null || request.ativa());

        return FaqResponse.from(faq);
    }

    @Transactional
    public void deletar(Long id, Usuario sindicoLogado) {
        Faq faq = buscarEntidade(id);
        validarAcessoCondominio(faq.getCondominio().getId(), sindicoLogado);
        faqRepository.delete(faq);
    }

    private Faq buscarEntidade(Long id) {
        return faqRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("FAQ não encontrada"));
    }

    private void validarAcessoCondominio(Long condominioId, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }
}
