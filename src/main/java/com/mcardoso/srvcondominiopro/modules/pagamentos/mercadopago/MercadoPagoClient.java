package com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago;

import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);
    private static final DateTimeFormatter EXPIRATION_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSxxx");

    private final RestClient restClient;
    private final MercadoPagoProperties properties;

    public MercadoPagoClient(RestClient.Builder builder, MercadoPagoProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    /** Cria um pagamento Pix e devolve o QR Code / dados da transação. */
    public MercadoPagoPayment criarPagamentoPix(PagamentoPixRequest request) {
        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", request.pagadorEmail());
        if (request.pagadorNome() != null && !request.pagadorNome().isBlank()) {
            payer.put("first_name", request.pagadorNome());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", request.valor());
        body.put("description", request.descricao());
        body.put("payment_method_id", "pix");
        body.put("payer", payer);
        body.put("external_reference", request.referenciaExterna());
        if (properties.notificationUrl() != null && !properties.notificationUrl().isBlank()) {
            body.put("notification_url", properties.notificationUrl());
        }
        if (properties.pixExpirationMinutes() > 0) {
            body.put("date_of_expiration", OffsetDateTime.now(ZoneOffset.of("-03:00"))
                    .plusMinutes(properties.pixExpirationMinutes())
                    .format(EXPIRATION_FORMAT));
        }

        try {
            return restClient.post()
                    .uri("/v1/payments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                    .header("X-Idempotency-Key", UUID.randomUUID().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MercadoPagoPayment.class);
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago recusou a criação da cobrança: {} - {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AppException("Não foi possível gerar a cobrança Pix no Mercado Pago", HttpStatus.BAD_GATEWAY);
        } catch (RestClientException ex) {
            log.error("Erro de comunicação com o Mercado Pago ao criar cobrança", ex);
            throw new AppException("Serviço de pagamentos indisponível no momento", HttpStatus.BAD_GATEWAY);
        }
    }

    /** Consulta o estado atual de um pagamento (usado pelo webhook). */
    public MercadoPagoPayment consultarPagamento(String paymentId) {
        try {
            return restClient.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                    .retrieve()
                    .body(MercadoPagoPayment.class);
        } catch (RestClientResponseException ex) {
            log.error("Mercado Pago recusou a consulta do pagamento {}: {} - {}",
                    paymentId, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AppException("Não foi possível consultar o pagamento no Mercado Pago", HttpStatus.BAD_GATEWAY);
        } catch (RestClientException ex) {
            log.error("Erro de comunicação com o Mercado Pago ao consultar pagamento {}", paymentId, ex);
            throw new AppException("Serviço de pagamentos indisponível no momento", HttpStatus.BAD_GATEWAY);
        }
    }
}
