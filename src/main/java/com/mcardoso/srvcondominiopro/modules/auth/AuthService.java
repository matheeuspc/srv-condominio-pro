package com.mcardoso.srvcondominiopro.modules.auth;

import com.mcardoso.srvcondominiopro.modules.auth.dto.AuthResponse;
import com.mcardoso.srvcondominiopro.modules.auth.dto.LoginRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.RegisterRequest;
import com.mcardoso.srvcondominiopro.modules.auth.dto.UsuarioResponse;
import com.mcardoso.srvcondominiopro.modules.condominios.Condominio;
import com.mcardoso.srvcondominiopro.modules.condominios.CondominioRepository;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.ConflictException;
import com.mcardoso.srvcondominiopro.shared.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final CondominioRepository condominioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            CondominioRepository condominioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.condominioRepository = condominioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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
}
