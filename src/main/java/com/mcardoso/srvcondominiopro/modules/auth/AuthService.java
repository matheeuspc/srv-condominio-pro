package com.mcardoso.srvcondominiopro.modules.auth;

import com.mcardoso.srvcondominiopro.modules.auth.dto.AuthResponse;
import com.mcardoso.srvcondominiopro.modules.auth.dto.ForgotPasswordRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.LoginRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.MessageResponse;
import com.mcardoso.srvcondominiopro.modules.auth.dto.RefreshTokenRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.RegisterRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.ResetPasswordRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.UsuarioResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.notificacoes.NotificacaoService;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ConflictException;
import com.mcardoso.srvcondominiopro.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final long RESET_SENHA_HORAS_VALIDADE = 1;
    private static final String FORGOT_PASSWORD_MSG =
            "Se o email estiver cadastrado, você receberá instruções para redefinir a senha.";

    private final UsuarioRepository usuarioRepository;
    private final CondominioRepository condominioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificacaoService notificacaoService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            CondominioRepository condominioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            NotificacaoService notificacaoService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.condominioRepository = condominioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificacaoService = notificacaoService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Já existe um usuário cadastrado com este email");
        }
        if (condominioRepository.existsByCnpj(request.cnpj())) {
            throw new ConflictException("Já existe um condomínio cadastrado com este CNPJ");
        }

        Condominio condominio = new Condominio();
        condominio.setNome(request.nomeCondominio());
        condominio.setCnpj(request.cnpj());
        condominio.setEndereco(request.endereco());
        condominio.setTelefone(request.telefoneCondominio());
        condominioRepository.save(condominio);

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setTelefone(request.telefone());
        usuario.setRole(Role.SINDICO);
        usuario.setCondominio(condominio);
        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario);
        return AuthResponse.of(token, UsuarioResponse.from(usuario));
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new BadCredentialsException("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new BadCredentialsException("Email ou senha inválidos");
        }

        String token = jwtService.generateToken(usuario);
        return AuthResponse.of(token, UsuarioResponse.from(usuario));
    }

    public UsuarioResponse me(Usuario usuarioLogado) {
        return UsuarioResponse.from(usuarioLogado);
    }

    /**
     * Gera um token de redefinição e notifica o usuário (best-effort). Resposta sempre neutra:
     * não revela se o email existe.
     */
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        usuarioRepository.findByEmail(request.email())
                .filter(Usuario::isAtivo)
                .ifPresent(usuario -> {
                    String token = UUID.randomUUID().toString();
                    usuario.setTokenResetSenha(token);
                    usuario.setTokenResetExpiracao(LocalDateTime.now().plusHours(RESET_SENHA_HORAS_VALIDADE));
                    notificacaoService.notificar(
                            usuario,
                            "Redefinição de senha",
                            ("Recebemos um pedido para redefinir sua senha. Use o token abaixo "
                                    + "(válido por %d hora):%n%n%s%n%nSe não foi você, ignore esta mensagem.")
                                    .formatted(RESET_SENHA_HORAS_VALIDADE, token));
                });
        return new MessageResponse(FORGOT_PASSWORD_MSG);
    }

    /** Redefine a senha via token e já devolve um JWT novo (mesmo fluxo do aceite de convite). */
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByTokenResetSenha(request.token())
                .filter(Usuario::isAtivo)
                .filter(u -> tokenValido(u.getTokenResetExpiracao(), LocalDateTime.now()))
                .orElseThrow(() -> new AppException(
                        "Token de redefinição inválido ou expirado", HttpStatus.BAD_REQUEST));

        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setTokenResetSenha(null);
        usuario.setTokenResetExpiracao(null);

        String token = jwtService.generateToken(usuario);
        return AuthResponse.of(token, UsuarioResponse.from(usuario));
    }

    /**
     * Renova um JWT ainda válido (sessão deslizante). Não há refresh token separado: um token
     * já expirado exige login. Token inválido/expirado ou usuário inativo → 401.
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtService.isTokenValid(request.token())) {
            throw new AppException("Token inválido ou expirado", HttpStatus.UNAUTHORIZED);
        }
        Long usuarioId = jwtService.extractUserId(request.token());
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new AppException("Token inválido ou expirado", HttpStatus.UNAUTHORIZED));

        String token = jwtService.generateToken(usuario);
        return AuthResponse.of(token, UsuarioResponse.from(usuario));
    }

    static boolean tokenValido(LocalDateTime expiracao, LocalDateTime referencia) {
        return expiracao != null && expiracao.isAfter(referencia);
    }
}
