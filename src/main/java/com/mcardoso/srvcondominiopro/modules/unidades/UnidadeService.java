package com.mcardoso.srvcondominiopro.modules.unidades;

import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.MoradorUnidadeRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.StatusMorador;
import com.mcardoso.srvcondominiopro.modules.unidades.dto.CreateUnidadeRequest;
import com.mcardoso.srvcondominiopro.modules.unidades.dto.UnidadeResponse;
import com.mcardoso.srvcondominiopro.modules.unidades.dto.UpdateUnidadeRequest;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.shared.exceptions.ConflictException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;
    private final CondominioRepository condominioRepository;
    private final MoradorUnidadeRepository moradorUnidadeRepository;

    public UnidadeService(
            UnidadeRepository unidadeRepository,
            CondominioRepository condominioRepository,
            MoradorUnidadeRepository moradorUnidadeRepository
    ) {
        this.unidadeRepository = unidadeRepository;
        this.condominioRepository = condominioRepository;
        this.moradorUnidadeRepository = moradorUnidadeRepository;
    }

    public List<UnidadeResponse> listar(Long condominioId, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        return unidadeRepository.findByCondominioIdOrderByBlocoAscNumeroAsc(condominioId).stream()
                .map(UnidadeResponse::from)
                .toList();
    }

    @Transactional
    public UnidadeResponse criar(Long condominioId, CreateUnidadeRequest request, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        validarDuplicidade(condominioId, request.bloco(), request.numero());

        Condominio condominio = condominioRepository.getReferenceById(condominioId);

        Unidade unidade = new Unidade();
        unidade.setBloco(request.bloco());
        unidade.setNumero(request.numero());
        unidade.setCondominio(condominio);
        unidadeRepository.save(unidade);

        return UnidadeResponse.from(unidade);
    }

    public UnidadeResponse buscar(Long id, Usuario usuarioLogado) {
        Unidade unidade = buscarPorId(id);
        validarAcessoUnidade(unidade, usuarioLogado);
        return UnidadeResponse.from(unidade);
    }

    @Transactional
    public UnidadeResponse atualizar(Long id, UpdateUnidadeRequest request, Usuario usuarioLogado) {
        Unidade unidade = buscarPorId(id);
        validarAcessoUnidade(unidade, usuarioLogado);

        boolean mudouChave = !Objects.equals(unidade.getBloco(), request.bloco())
                || !unidade.getNumero().equals(request.numero());
        if (mudouChave) {
            validarDuplicidade(unidade.getCondominio().getId(), request.bloco(), request.numero());
        }

        unidade.setBloco(request.bloco());
        unidade.setNumero(request.numero());

        return UnidadeResponse.from(unidade);
    }

    @Transactional
    public void deletar(Long id, Usuario usuarioLogado) {
        Unidade unidade = buscarPorId(id);
        validarAcessoUnidade(unidade, usuarioLogado);

        if (moradorUnidadeRepository.existsByUnidadeIdAndStatus(id, StatusMorador.ATIVO)) {
            throw new ConflictException("Não é possível excluir uma unidade com moradores ativos");
        }

        unidadeRepository.delete(unidade);
    }

    private Unidade buscarPorId(Long id) {
        return unidadeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Unidade não encontrada"));
    }

    private void validarDuplicidade(Long condominioId, String bloco, String numero) {
        if (unidadeRepository.existsByCondominioIdAndBlocoAndNumero(condominioId, bloco, numero)) {
            throw new ConflictException("Já existe uma unidade com esse bloco/número neste condomínio");
        }
    }

    private void validarAcessoCondominio(Long condominioId, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }

    private void validarAcessoUnidade(Unidade unidade, Usuario usuarioLogado) {
        validarAcessoCondominio(unidade.getCondominio().getId(), usuarioLogado);
    }
}
