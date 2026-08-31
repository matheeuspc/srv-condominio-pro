package com.mcardoso.srvcondominiopro.modules.pagamentos.mercadopago;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.mercadopago")
public record MercadoPagoProperties(
        // Access token da conta Mercado Pago. Vazio em ambiente local => integração desligada
        // (criar-cobranca responde 503, webhook é apenas logado).
        String accessToken,

        // Segredo da assinatura do webhook (painel do MP). Vazio => assinatura não é validada.
        String webhookSecret,

        @DefaultValue("https://api.mercadopago.com")
        String baseUrl,

        // URL pública deste serviço que o MP deve chamar; opcional (também dá pra configurar no painel).
        String notificationUrl,

        @DefaultValue("30")
        int pixExpirationMinutes
) {
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }
}
