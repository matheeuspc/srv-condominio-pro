package com.mcardoso.srvcondominiopro.modules.moradores;

import com.mcardoso.srvcondominiopro.modules.auth.dto.AuthResponse;
import com.mcardoso.srvcondominiopro.modules.auth.dto.UsuarioResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.AceitarConviteRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.CreateMoradorRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.MoradorResponse;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.UpdateMoradorRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.VincularUnidadeRequest;
import com.mcardoso.srvcondominiopro.modules.moradores.dto.VinculoResponse;
import com.mcardoso.srvcondominiopro.modules.notificacoes.NotificacaoService;
import com.mcardoso.srvcondominiopro.modules.unidades.Unidade;
import com.mcardoso.srvcondominiopro.modules.unidades.UnidadeRepository;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ConflictException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import com.mcardoso.srvcondominiopro.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MoradorService {

    private static final long TOKEN_CONVITE_DIAS_VALIDADE = 7;

    private final UsuarioRepository usuarioRepository;
    private final UnidadeRepository unidadeRepository;
    private final MoradorUnidadeRepository moradorUnidadeRepository;
    private final CondominioRepository condominioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificacaoService notificacaoService;

    public MoradorService(
            UsuarioRepository usuarioRepository,
            UnidadeRepository unidadeRepository,
            MoradorUnidadeRepository moradorUnidadeRepository,
            CondominioRepository condominioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NotificacaoService notificacaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.unidadeRepository = unidadeRepository;
        this.moradorUnidadeRepository = moradorUnidadeRepository;
        this.condominioRepository = condominioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificacaoService = notificacaoService;
    }

    public List<MoradorResponse> listar(Long condominioId, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        return usuarioRepository
                .findByCondominioIdAndRoleInOrderByNomeAsc(condominioId, List.of(Role.PROPRIETARIO, Role.INQUILINO))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MoradorResponse criar(Long condominioId, CreateMoradorRequest request, Usuario usuarioLogado) {
        validarAcessoCondominio(condominioId, usuarioLogado);
        validarRoleMorador(request.role());

        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com este email");
        }

        Condominio condominio = condominioRepository.getReferenceById(condominioId);

        Usuario morador = new Usuario();
        morador.setNome(request.nome());
        morador.setEmail(request.email());
        morador.setTelefone(request.telefone());
        morador.setRole(request.role());
        morador.setAtivo(true);
        morador.setCondominio(condominio);
        // Senha placeholder inutilizável até o morador aceitar o convite e definir a própria senha.
        morador.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        morador.setTokenConvite(UUID.randomUUID().toString());
        morador.setTokenExpiracao(LocalDateTime.now().plusDays(TOKEN_CONVITE_DIAS_VALIDADE));
        usuarioRepository.save(morador);

        // Convite por notificação (best-effort). O token também segue na resposta como fallback
        // caso nenhum canal esteja configurado.
        notificacaoService.notificar(
                morador,
                "Convite para o " + condominio.getNome(),
                ("Você foi cadastrado no %s. Use o token de convite abaixo para definir sua senha "
                        + "(válido por %d dias):%n%n%s")
                        .formatted(condominio.getNome(), TOKEN_CONVITE_DIAS_VALIDADE, morador.getTokenConvite()));

        return toResponse(morador);
    }

    public MoradorResponse buscar(Long id, Usuario usuarioLogado) {
        Usuario morador = buscarMoradorPorId(id);
        validarAcessoCondominio(morador.getCondominio().getId(), usuarioLogado);
        return toResponse(morador);
    }

    @Transactional
    public MoradorResponse atualizar(Long id, UpdateMoradorRequest request, Usuario usuarioLogado) {
        Usuario morador = buscarMoradorPorId(id);
        validarAcessoCondominio(morador.getCondominio().getId(), usuarioLogado);

        morador.setNome(request.nome());
        morador.setTelefone(request.telefone());

        return toResponse(morador);
    }

    @Transactional
    public void desativar(Long id, Usuario usuarioLogado) {
        Usuario morador = buscarMoradorPorId(id);
        validarAcessoCondominio(morador.getCondominio().getId(), usuarioLogado);

        morador.setAtivo(false);

        LocalDateTime agora = LocalDateTime.now();
        List<MoradorUnidade> vinculosAtivos = moradorUnidadeRepository.findByUsuarioIdAndStatus(id, StatusMorador.ATIVO);
        for (MoradorUnidade vinculo : vinculosAtivos) {
            vinculo.setStatus(StatusMorador.INATIVO);
            vinculo.setDataFim(agora);
        }
    }

    @Transactional
    public MoradorResponse vincularUnidade(Long id, VincularUnidadeRequest request, Usuario usuarioLogado) {
        Usuario morador = buscarMoradorPorId(id);
        validarAcessoCondominio(morador.getCondominio().getId(), usuarioLogado);
        validarRoleMorador(request.tipo());

        Unidade unidade = unidadeRepository.findById(request.unidadeId())
                .orElseThrow(() -> new NotFoundException("Unidade não encontrada"));
        if (!unidade.getCondominio().getId().equals(morador.getCondominio().getId())) {
            throw new ForbiddenException("A unidade não pertence ao mesmo condomínio do morador");
        }
        if (moradorUnidadeRepository.existsByUsuarioIdAndUnidadeId(id, request.unidadeId())) {
            throw new ConflictException("Morador já vinculado a esta unidade");
        }

        MoradorUnidade vinculo = new MoradorUnidade();
        vinculo.setUsuario(morador);
        vinculo.setUnidade(unidade);
        vinculo.setTipo(request.tipo());
        vinculo.setStatus(StatusMorador.ATIVO);
        moradorUnidadeRepository.save(vinculo);

        return toResponse(morador);
    }

    public MoradorResponse me(Usuario usuarioLogado) {
        return toResponse(usuarioLogado);
    }

    @Transactional
    public AuthResponse aceitarConvite(String token, AceitarConviteRequest request) {
        Usuario morador = usuarioRepository.findByTokenConvite(token)
                .filter(u -> u.getTokenExpiracao() != null && u.getTokenExpiracao().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new AppException("Convite inválido ou expirado", HttpStatus.BAD_REQUEST));

        morador.setSenha(passwordEncoder.encode(request.senha()));
        morador.setTokenConvite(null);
        morador.setTokenExpiracao(null);

        String jwt = jwtService.generateToken(morador);
        return AuthResponse.of(jwt, UsuarioResponse.from(morador));
    }

    private Usuario buscarMoradorPorId(Long id) {
        return usuarioRepository.findById(id)
                .filter(u -> u.getRole() == Role.PROPRIETARIO || u.getRole() == Role.INQUILINO)
                .orElseThrow(() -> new NotFoundException("Morador não encontrado"));
    }

    private void validarRoleMorador(Role role) {
        if (role != Role.PROPRIETARIO && role != Role.INQUILINO) {
            throw new AppException("Role deve ser PROPRIETARIO ou INQUILINO", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarAcessoCondominio(Long condominioId, Usuario usuarioLogado) {
        if (!usuarioLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }
    }

    private MoradorResponse toResponse(Usuario morador) {
        List<VinculoResponse> unidades = moradorUnidadeRepository.findByUsuarioId(morador.getId()).stream()
                .map(VinculoResponse::from)
                .toList();

        return new MoradorResponse(
                morador.getId(),
                morador.getNome(),
                morador.getEmail(),
                morador.getTelefone(),
                morador.getRole(),
                morador.isAtivo(),
                morador.getCondominio().getId(),
                morador.getTokenConvite(),
                unidades
        );
    }
}
