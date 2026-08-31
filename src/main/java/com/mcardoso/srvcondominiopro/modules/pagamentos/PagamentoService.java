package com.mcardoso.srvcondominiopro.modules.pagamentos;

import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.CriarCobrancaRequest;
import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.MercadoPagoWebhookRequest;
import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.PagamentoResponse;
import com.mcardoso.srvcondominiopro.modules.pagamentos.dto.RelatorioFinanceiroResponse;
import com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago.MercadoPagoClient;
import com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago.MercadoPagoPayment;
import com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago.MercadoPagoWebhookValidator;
import com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago.PagamentoPixRequest;
import com.mcardoso.srvcondominiopro.modules.reservas.Reserva;
import com.mcardoso.srvcondominiopro.modules.reservas.ReservaRepository;
import com.mcardoso.srvcondominiopro.modules.reservas.StatusReserva;
import com.mcardoso.srvcondominiopro.modules.usuarios.Role;
import com.mcardoso.srvcondominiopro.modules.usuarios.Usuario;
import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ConflictException;
import com.mcardoso.srvcondominiopro.shared.exceptions.ForbiddenException;
import com.mcardoso.srvcondominiopro.shared.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PagamentoService {

    private static final Logger log = LoggerFactory.getLogger(PagamentoService.class);

    private final PagamentoRepository pagamentoRepository;
    private final ReservaRepository reservaRepository;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoWebhookValidator webhookValidator;

    public PagamentoService(
            PagamentoRepository pagamentoRepository,
            ReservaRepository reservaRepository,
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoWebhookValidator webhookValidator
    ) {
        this.pagamentoRepository = pagamentoRepository;
        this.reservaRepository = reservaRepository;
        this.mercadoPagoClient = mercadoPagoClient;
        this.webhookValidator = webhookValidator;
    }

    @Transactional
    public PagamentoResponse criarCobranca(CriarCobrancaRequest request, Usuario moradorLogado) {
        Reserva reserva = reservaRepository.findById(request.reservaId())
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada"));

        if (!reserva.getUsuario().getId().equals(moradorLogado.getId())) {
            throw new ForbiddenException("Você não tem acesso a esta reserva");
        }

        BigDecimal taxa = reserva.getArea().getTaxa();
        if (taxa == null || taxa.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("Esta reserva não possui taxa a pagar", HttpStatus.BAD_REQUEST);
        }
        if (reserva.getStatus() != StatusReserva.PENDENTE) {
            throw new AppException("Só é possível gerar cobrança para reservas pendentes", HttpStatus.BAD_REQUEST);
        }

        Pagamento pagamento = pagamentoRepository.findByReservaId(reserva.getId()).orElse(null);
        if (pagamento != null) {
            if (pagamento.getStatus() == StatusPagamento.PAGO) {
                throw new ConflictException("Esta reserva já foi paga");
            }
            // Cobrança pendente já emitida: devolve a mesma (idempotente), sem gerar outra no MP.
            if (pagamento.getStatus() == StatusPagamento.AGUARDANDO && pagamento.getMpQrCode() != null) {
                return PagamentoResponse.from(pagamento);
            }
        }

        if (!mercadoPagoClient.isConfigured()) {
            throw new AppException("Serviço de pagamentos indisponível no momento", HttpStatus.SERVICE_UNAVAILABLE);
        }

        MercadoPagoPayment mp = mercadoPagoClient.criarPagamentoPix(new PagamentoPixRequest(
                taxa,
                "Reserva %s - %s".formatted(reserva.getArea().getNome(), reserva.getData()),
                moradorLogado.getEmail(),
                moradorLogado.getNome(),
                "reserva-" + reserva.getId()
        ));

        if (pagamento == null) {
            pagamento = new Pagamento();
            pagamento.setReserva(reserva);
        }
        pagamento.setValor(taxa);
        pagamento.setMetodoPagamento("PIX");
        pagamento.setMpPaymentId(mp.paymentId());
        pagamento.setMpQrCode(mp.qrCode());
        pagamento.setMpQrCodeBase64(mp.qrCodeBase64());
        pagamento.setMpTicketUrl(mp.ticketUrl());
        pagamento.setStatus(mapStatus(mp.status()));
        if (pagamento.getStatus() == StatusPagamento.PAGO) {
            pagamento.setPaidAt(LocalDateTime.now());
            confirmarReserva(reserva);
        }
        pagamentoRepository.save(pagamento);

        return PagamentoResponse.from(pagamento);
    }

    public PagamentoResponse consultarStatus(Long id, Usuario usuarioLogado) {
        Pagamento pagamento = pagamentoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado"));
        validarAcesso(pagamento.getReserva(), usuarioLogado);
        return PagamentoResponse.from(pagamento);
    }

    public PagamentoResponse buscarPorReserva(Long reservaId, Usuario usuarioLogado) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new NotFoundException("Reserva não encontrada"));
        validarAcesso(reserva, usuarioLogado);
        Pagamento pagamento = pagamentoRepository.findByReservaId(reservaId)
                .orElseThrow(() -> new NotFoundException("Esta reserva não possui pagamento"));
        return PagamentoResponse.from(pagamento);
    }

    public RelatorioFinanceiroResponse relatorio(Long condominioId, Usuario sindicoLogado) {
        if (!sindicoLogado.getCondominio().getId().equals(condominioId)) {
            throw new ForbiddenException("Você não tem acesso a este condomínio");
        }

        List<Pagamento> pagamentos =
                pagamentoRepository.findByReservaAreaCondominioIdOrderByCreatedAtDesc(condominioId);

        BigDecimal totalRecebido = somarPorStatus(pagamentos, StatusPagamento.PAGO);
        BigDecimal totalAguardando = somarPorStatus(pagamentos, StatusPagamento.AGUARDANDO);
        long confirmados = pagamentos.stream()
                .filter(p -> p.getStatus() == StatusPagamento.PAGO)
                .count();

        return new RelatorioFinanceiroResponse(
                condominioId,
                totalRecebido,
                totalAguardando,
                pagamentos.size(),
                confirmados,
                pagamentos.stream().map(PagamentoResponse::from).toList()
        );
    }

    @Transactional
    public void processarWebhook(
            String type, String dataId, MercadoPagoWebhookRequest body, String signature, String requestId) {

        String tipo = type != null ? type : (body != null ? body.type() : null);
        String paymentId = dataId != null ? dataId : (body != null ? body.paymentId() : null);

        if (!webhookValidator.isValid(paymentId, requestId, signature)) {
            throw new AppException("Assinatura do webhook inválida", HttpStatus.FORBIDDEN);
        }

        // O MP manda vários tópicos no mesmo endpoint; só pagamentos interessam aqui.
        if (tipo != null && !"payment".equals(tipo)) {
            return;
        }
        if (paymentId == null || paymentId.isBlank()) {
            log.warn("Webhook do Mercado Pago sem id de pagamento — ignorado");
            return;
        }
        if (!mercadoPagoClient.isConfigured()) {
            log.warn("Webhook recebido mas integração Mercado Pago não configurada — ignorado (payment {})", paymentId);
            return;
        }

        MercadoPagoPayment mp = mercadoPagoClient.consultarPagamento(paymentId);

        Pagamento pagamento = pagamentoRepository.findByMpPaymentId(paymentId).orElse(null);
        if (pagamento == null) {
            log.warn("Webhook do Mercado Pago para pagamento desconhecido: {}", paymentId);
            return;
        }

        StatusPagamento novoStatus = mapStatus(mp.status());
        if (novoStatus == pagamento.getStatus()) {
            return;
        }

        pagamento.setStatus(novoStatus);
        if (novoStatus == StatusPagamento.PAGO) {
            pagamento.setPaidAt(LocalDateTime.now());
            confirmarReserva(pagamento.getReserva());
        }
        pagamentoRepository.save(pagamento);
        log.info("Pagamento {} (reserva {}) atualizado para {}",
                paymentId, pagamento.getReserva().getId(), novoStatus);
    }

    // --- helpers ---

    private void confirmarReserva(Reserva reserva) {
        if (reserva.getStatus() == StatusReserva.PENDENTE) {
            reserva.setStatus(StatusReserva.CONFIRMADA);
            reservaRepository.save(reserva);
        }
    }

    private void validarAcesso(Reserva reserva, Usuario usuarioLogado) {
        Long condominioReserva = reserva.getArea().getCondominio().getId();
        if (usuarioLogado.getRole() == Role.SINDICO) {
            if (!usuarioLogado.getCondominio().getId().equals(condominioReserva)) {
                throw new ForbiddenException("Você não tem acesso a este pagamento");
            }
            return;
        }
        if (!reserva.getUsuario().getId().equals(usuarioLogado.getId())) {
            throw new ForbiddenException("Você não tem acesso a este pagamento");
        }
    }

    private static BigDecimal somarPorStatus(List<Pagamento> pagamentos, StatusPagamento status) {
        return pagamentos.stream()
                .filter(p -> p.getStatus() == status)
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static StatusPagamento mapStatus(String mpStatus) {
        if (mpStatus == null) {
            return StatusPagamento.AGUARDANDO;
        }
        return switch (mpStatus) {
            case "approved" -> StatusPagamento.PAGO;
            case "rejected", "cancelled" -> StatusPagamento.FALHOU;
            case "refunded", "charged_back" -> StatusPagamento.ESTORNADO;
            default -> StatusPagamento.AGUARDANDO;
        };
    }
}
