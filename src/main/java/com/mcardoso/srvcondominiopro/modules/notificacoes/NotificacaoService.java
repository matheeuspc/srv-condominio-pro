package com.mcardoso.srvcondominiopro.modules.notificacoes;

import com.mcardoso.srvcondominiopro.modules.notificacoes.canais.EmailSender;
import com.mcardoso.srvcondominiopro.modules.notificacoes.canais.WhatsappSender;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.AtualizarPreferenciasRequest;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.EnviarNotificacaoRequest;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.EnvioResultadoResponse;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.NotificacaoResponse;
import com.mcardoso.srvcondominiopro.modules.notificacoes.dto.PreferenciasResponse;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.modules.usuarios.UsuarioRepository;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final int MAX_ERRO = 1000;

    private final NotificacaoRepository notificacaoRepository;
    private final PreferenciaNotificacaoRepository preferenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EmailSender emailSender;
    private final WhatsappSender whatsappSender;

    public NotificacaoService(
            NotificacaoRepository notificacaoRepository,
            PreferenciaNotificacaoRepository preferenciaRepository,
            UsuarioRepository usuarioRepository,
            EmailSender emailSender,
            WhatsappSender whatsappSender
    ) {
        this.notificacaoRepository = notificacaoRepository;
        this.preferenciaRepository = preferenciaRepository;
        this.usuarioRepository = usuarioRepository;
        this.emailSender = emailSender;
        this.whatsappSender = whatsappSender;
    }

    // ------------------------------------------------------------------
    // API de eventos (novo comunicado, reserva confirmada/cancelada, convite).
    // Best-effort: nunca lança, para não derrubar a transação do evento de origem.
    // Se nenhum canal estiver configurado, é um no-op silencioso (não grava nada).
    // ------------------------------------------------------------------
    public void notificar(Usuario destinatario, String assunto, String conteudo) {
        for (TipoNotificacao canal : resolverCanais(destinatario)) {
            registrarEEnviar(destinatario, canal, assunto, conteudo);
        }
    }

    // ------------------------------------------------------------------
    // Preferências do morador (CONTEXT 6.10).
    // ------------------------------------------------------------------
    public PreferenciasResponse verPreferencias(Usuario morador) {
        return preferenciaRepository.findByUsuarioId(morador.getId())
                .map(PreferenciasResponse::from)
                .orElseGet(() -> PreferenciasResponse.padrao(morador.getId()));
    }

    @Transactional
    public PreferenciasResponse atualizarPreferencias(Usuario morador, AtualizarPreferenciasRequest request) {
        PreferenciaNotificacao pref = preferenciaRepository.findByUsuarioId(morador.getId())
                .orElseGet(() -> {
                    PreferenciaNotificacao nova = new PreferenciaNotificacao();
                    nova.setUsuario(morador);
                    return nova;
                });
        if (request.notificarEmail() != null) {
            pref.setNotificarEmail(request.notificarEmail());
        }
        if (request.notificarWhatsapp() != null) {
            pref.setNotificarWhatsapp(request.notificarWhatsapp());
        }
        preferenciaRepository.save(pref);
        return PreferenciasResponse.from(pref);
    }

    // ------------------------------------------------------------------
    // Log de envios (CONTEXT 6.10) — SINDICO.
    // ------------------------------------------------------------------
    public List<NotificacaoResponse> log(
            Long condominioId, Usuario sindicoLogado, TipoNotificacao tipo, StatusNotificacao status) {
        if (!sindicoLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }

        List<Notificacao> notificacoes;
        if (tipo != null && status != null) {
            notificacoes = notificacaoRepository
                    .findByUsuarioCondominioIdAndTipoAndStatusOrderByCreatedAtDesc(condominioId, tipo, status);
        } else if (tipo != null) {
            notificacoes = notificacaoRepository
                    .findByUsuarioCondominioIdAndTipoOrderByCreatedAtDesc(condominioId, tipo);
        } else if (status != null) {
            notificacoes = notificacaoRepository
                    .findByUsuarioCondominioIdAndStatusOrderByCreatedAtDesc(condominioId, status);
        } else {
            notificacoes = notificacaoRepository.findByUsuarioCondominioIdOrderByCreatedAtDesc(condominioId);
        }
        return notificacoes.stream().map(NotificacaoResponse::from).toList();
    }

    // ------------------------------------------------------------------
    // Envio manual (CONTEXT 6.10) — SINDICO.
    // ------------------------------------------------------------------
    @Transactional
    public EnvioResultadoResponse enviarManual(EnviarNotificacaoRequest request, Usuario sindicoLogado) {
        if (!emailSender.isConfigured() && !whatsappSender.isConfigured()) {
            throw new AppException("Nenhum canal de notificação configurado", HttpStatus.SERVICE_UNAVAILABLE);
        }

        List<Usuario> alvos = resolverAlvos(request, sindicoLogado);
        if (alvos.isEmpty()) {
            throw new AppException("Nenhum destinatário encontrado para os critérios informados", HttpStatus.BAD_REQUEST);
        }

        int enviados = 0;
        int falhas = 0;
        for (Usuario alvo : alvos) {
            List<TipoNotificacao> canais = request.tipo() != null
                    ? List.of(request.tipo())
                    : new ArrayList<>(resolverCanais(alvo));
            if (canais.isEmpty()) {
                falhas++;
                continue;
            }
            for (TipoNotificacao canal : canais) {
                boolean ok = registrarEEnviar(alvo, canal, request.assunto(), request.conteudo());
                if (ok) {
                    enviados++;
                } else {
                    falhas++;
                }
            }
        }
        return new EnvioResultadoResponse(alvos.size(), enviados, falhas);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private List<Usuario> resolverAlvos(EnviarNotificacaoRequest request, Usuario sindicoLogado) {
        Long condominioId = sindicoLogado.getCondominio().getId();

        if (request.usuarioIds() != null && !request.usuarioIds().isEmpty()) {
            List<Usuario> usuarios = usuarioRepository.findAllById(request.usuarioIds());
            for (Usuario u : usuarios) {
                if (!u.getCondominio().getId().equals(condominioId)) {
                    throw new ForbiddenException("Usuário " + u.getId() + " não pertence ao seu condomínio");
                }
            }
            return usuarios;
        }

        List<Role> roles = request.destinatario() != null
                ? List.of(request.destinatario())
                : List.of(Role.PROPRIETARIO, Role.INQUILINO);
        return usuarioRepository.findByCondominioIdAndAtivoTrueAndRoleInOrderByNomeAsc(condominioId, roles);
    }

    private Set<TipoNotificacao> resolverCanais(Usuario usuario) {
        boolean prefEmail = true;
        boolean prefWhatsapp = false;
        PreferenciaNotificacao pref = preferenciaRepository.findByUsuarioId(usuario.getId()).orElse(null);
        if (pref != null) {
            prefEmail = pref.isNotificarEmail();
            prefWhatsapp = pref.isNotificarWhatsapp();
        }
        boolean whatsappHabilitadoNoCondominio = usuario.getCondominio().isNotificaWhatsapp();
        boolean temTelefone = usuario.getTelefone() != null && !usuario.getTelefone().isBlank();

        return canais(
                prefEmail,
                prefWhatsapp && whatsappHabilitadoNoCondominio,
                emailSender.isConfigured(),
                whatsappSender.isConfigured(),
                temTelefone);
    }

    /** Resolve os canais efetivos para um envio, cruzando preferência × configuração × dados do usuário. */
    static Set<TipoNotificacao> canais(
            boolean prefEmail,
            boolean prefWhatsapp,
            boolean emailConfigurado,
            boolean whatsappConfigurado,
            boolean temTelefone) {
        Set<TipoNotificacao> canais = EnumSet.noneOf(TipoNotificacao.class);
        if (prefEmail && emailConfigurado) {
            canais.add(TipoNotificacao.EMAIL);
        }
        if (prefWhatsapp && whatsappConfigurado && temTelefone) {
            canais.add(TipoNotificacao.WHATSAPP);
        }
        return canais;
    }

    private boolean registrarEEnviar(Usuario usuario, TipoNotificacao canal, String assunto, String conteudo) {
        Notificacao notificacao = new Notificacao();
        notificacao.setUsuario(usuario);
        notificacao.setTipo(canal);
        notificacao.setAssunto(assunto);
        notificacao.setConteudo(conteudo);
        notificacao.setStatus(StatusNotificacao.PENDENTE);

        boolean enviado = false;
        try {
            if (canal == TipoNotificacao.EMAIL) {
                emailSender.enviar(usuario.getEmail(), assunto, conteudo);
            } else {
                whatsappSender.enviar(usuario.getTelefone(), conteudo);
            }
            notificacao.setStatus(StatusNotificacao.ENVIADO);
            notificacao.setEnviadoEm(LocalDateTime.now());
            enviado = true;
        } catch (RuntimeException ex) {
            notificacao.setStatus(StatusNotificacao.FALHOU);
            notificacao.setErro(abreviar(ex.getMessage()));
            log.warn("Falha ao enviar notificação {} para usuário {}: {}", canal, usuario.getId(), ex.getMessage());
        }
        notificacaoRepository.save(notificacao);
        return enviado;
    }

    private static String abreviar(String texto) {
        if (texto == null) {
            return "erro sem mensagem";
        }
        return texto.length() <= MAX_ERRO ? texto : texto.substring(0, MAX_ERRO);
    }
}
