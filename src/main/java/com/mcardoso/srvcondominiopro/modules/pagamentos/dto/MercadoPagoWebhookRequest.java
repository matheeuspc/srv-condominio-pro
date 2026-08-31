package com.mcardoso.srvcondominiopro.modules.pagamentos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Corpo enviado pelo Mercado Pago nas notificações de webhook. O id do pagamento pode chegar
// tanto no corpo (data.id) quanto na query string (?data.id=), então o controller aceita os dois.
@JsonIgnoreProperties(ignoreUnknown = true)
public record MercadoPagoWebhookRequest(
        String type,
        String action,
        Data data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String id) {
    }

    public String paymentId() {
        return data != null ? data.id() : null;
    }
}
