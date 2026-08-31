package com.mcardoso.srvcondominiopro.modules.avisos;

import com.mcardoso.srvcondominiopro.modules.avisos.dto.AvisoResponse;
import com.mcardoso.srvcondominiopro.modules.avisos.dto.CreateAvisoRequest;
import com.mcardoso.srvcondominiopro.modules.avisos.dto.UpdateAvisoRequest;
import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.notificacoes.NotificacaoService;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AvisoService {

    private final AvisoRepository avisoRepository;
    private final AvisoLeituraRepository avisoLeituraRepository;
    private final CondominioRepository condominioRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacaoService notificacaoService;

    public AvisoService(
            AvisoRepository avisoRepository,
            AvisoLeituraRepository avisoLeituraRepository,
            CondominioRepository condominioRepository,
            UsuarioRepository usuarioRepository,
            NotificacaoService notificacaoService
    ) {
        this.avisoRepository = avisoRepository;
        this.avisoLeituraRepository = avisoLeituraRepository;
        this.condominioRepository = condominioRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
    }

    public List<AvisoResponse> listar(Long condominioId, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);

        if (usuarioLogado.getRole() == Role.SINDICO) {
            return avisoRepository.findByCondominioIdOrderByCreatedAtDesc(condominioId).stream()
                    .map(AvisoResponse::from)
                    .toList();
        }

        Set<Long> lidos = new HashSet<>(
                avisoLeituraRepository.findAvisoIdsLidosPorUsuario(usuarioLogado.getId()));
        return avisoRepository.findVisiveisParaMorador(condominioId, usuarioLogado.getRole()).stream()
                .map(a -> AvisoResponse.from(a, lidos.contains(a.getId())))
                .toList();
    }

    @Transactional
    public AvisoResponse criar(Long condominioId, CreateAvisoRequest request, Usuario sindicoLogado) {
        validarAcessoCondominio(condominioId, sindicoLogado);
        validarDestinatario(request.destinatario());

        Condominio condominio = condominioRepository.getReferenceById(condominioId);

        Aviso aviso = new Aviso();
        aviso.setTitulo(request.titulo());
        aviso.setConteudo(request.conteudo());
        aviso.setAnexoUrl(request.anexoUrl());
        aviso.setPublicado(request.publicado() == null || request.publicado());
        aviso.setDestinatario(request.destinatario());
        aviso.setCondominio(condominio);
        aviso.setAutor(sindicoLogado);
        avisoRepository.save(aviso);

        if (aviso.isPublicado()) {
            notificarMoradores(aviso);
        }
        return AvisoResponse.from(aviso);
    }

    public AvisoResponse buscar(Long id, Usuario usuarioLogado) {
        Aviso aviso = buscarPorId(id);
        validarAcessoCondominio(aviso.getCondominio().getId(), usuarioLogado);

        boolean lido = false;
        if (usuarioLogado.getRole() != Role.SINDICO) {
            if (!visivelParaMorador(aviso.getDestinatario(), usuarioLogado.getRole(), aviso.isPublicado())) {
                throw new NotFoundException("Aviso não encontrado");
            }
            lido = avisoLeituraRepository.existsByAvisoIdAndUsuarioId(id, usuarioLogado.getId());
        }
        return AvisoResponse.from(aviso, lido);
    }

    @Transactional
    public AvisoResponse atualizar(Long id, UpdateAvisoRequest request, Usuario sindicoLogado) {
        Aviso aviso = buscarPorId(id);
        validarAcessoCondominio(aviso.getCondominio().getId(), sindicoLogado);
        validarDestinatario(request.destinatario());

        aviso.setTitulo(request.titulo());
        aviso.setConteudo(request.conteudo());
        aviso.setAnexoUrl(request.anexoUrl());
        aviso.setPublicado(request.publicado() == null || request.publicado());
        aviso.setDestinatario(request.destinatario());

        return AvisoResponse.from(aviso);
    }

    @Transactional
    public void deletar(Long id, Usuario sindicoLogado) {
        Aviso aviso = buscarPorId(id);
        validarAcessoCondominio(aviso.getCondominio().getId(), sindicoLogado);
        avisoLeituraRepository.deleteByAvisoId(id);
        avisoRepository.delete(aviso);
    }

    @Transactional
    public void marcarLido(Long id, Usuario moradorLogado) {
        Aviso aviso = buscarPorId(id);
        validarAcessoCondominio(aviso.getCondominio().getId(), moradorLogado);
        if (!visivelParaMorador(aviso.getDestinatario(), moradorLogado.getRole(), aviso.isPublicado())) {
            throw new NotFoundException("Aviso não encontrado");
        }
        if (avisoLeituraRepository.existsByAvisoIdAndUsuarioId(id, moradorLogado.getId())) {
            return; // idempotente
        }
        AvisoLeitura leitura = new AvisoLeitura();
        leitura.setAviso(aviso);
        leitura.setUsuarioId(moradorLogado.getId());
        leitura.setLido(true);
        avisoLeituraRepository.save(leitura);
    }

    public List<AvisoResponse> naoLidos(Usuario moradorLogado) {
        Set<Long> lidos = new HashSet<>(
                avisoLeituraRepository.findAvisoIdsLidosPorUsuario(moradorLogado.getId()));
        return avisoRepository
                .findVisiveisParaMorador(moradorLogado.getCondominio().getId(), moradorLogado.getRole()).stream()
                .filter(a -> !lidos.contains(a.getId()))
                .map(a -> AvisoResponse.from(a, false))
                .toList();
    }

    /** Regra de visibilidade de um aviso para um morador (CONTEXT 5.3). */
    static boolean visivelParaMorador(Role destinatario, Role roleMorador, boolean publicado) {
        return publicado && (destinatario == null || destinatario == roleMorador);
    }

    private void notificarMoradores(Aviso aviso) {
        List<Role> alvo = aviso.getDestinatario() == null
                ? List.of(Role.PROPRIETARIO, Role.INQUILINO)
                : List.of(aviso.getDestinatario());
        List<Usuario> moradores = usuarioRepository
                .findByCondominioIdAndAtivoTrueAndRoleInOrderByNomeAsc(aviso.getCondominio().getId(), alvo);

        String assunto = "Novo comunicado: " + aviso.getTitulo();
        for (Usuario morador : moradores) {
            notificacaoService.notificar(morador, assunto, aviso.getConteudo());
        }
    }

    private void validarDestinatario(Role destinatario) {
        if (destinatario == Role.SINDICO) {
            throw new AppException("destinatario deve ser PROPRIETARIO, INQUILINO ou vazio", HttpStatus.BAD_REQUEST);
        }
    }

    private Aviso buscarPorId(Long id) {
        return avisoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Aviso não encontrado"));
    }

    private void validarAcessoCondominio(Long condominioId, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }
}
