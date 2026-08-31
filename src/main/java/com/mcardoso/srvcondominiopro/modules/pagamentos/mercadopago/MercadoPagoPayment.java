package com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Recorte da resposta do Mercado Pago em POST/GET /v1/payments — só o que este módulo usa.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoPayment(
        Long id,
        String status,
        @JsonProperty("status_detail") String statusDetail,
        @JsonProperty("external_reference") String externalReference,
        @JsonProperty("point_of_interaction") PointOfInteraction pointOfInteraction
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PointOfInteraction(@JsonProperty("transaction_data") TransactionData transactionData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TransactionData(
            @JsonProperty("qr_code") String qrCode,
            @JsonProperty("qr_code_base64") String qrCodeBase64,
            @JsonProperty("ticket_url") String ticketUrl
    ) {
    }

    public String paymentId() {
        return id != null ? String.valueOf(id) : null;
    }

    private TransactionData transactionData() {
        return pointOfInteraction != null ? pointOfInteraction.transactionData() : null;
    }

    public String qrCode() {
        TransactionData td = transactionData();
        return td != null ? td.qrCode() : null;
    }

    public String qrCodeBase64() {
        TransactionData td = transactionData();
        return td != null ? td.qrCodeBase64() : null;
    }

    public String ticketUrl() {
        TransactionData td = transactionData();
        return td != null ? td.ticketUrl() : null;
    }
}
