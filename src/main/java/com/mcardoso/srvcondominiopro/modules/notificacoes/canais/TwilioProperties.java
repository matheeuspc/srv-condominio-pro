package com.mcardoso.srvcondominiopro.modules.notificacoes.canais;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Config do provedor de WhatsApp (Twilio). Sem account-sid/auth-token/from => canal desligado.
@ConfigurationProperties(prefix = "app.twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        // Número remetente no formato "whatsapp:+55...".
        String from,

        @DefaultValue("https://api.twilio.com")
        String baseUrl
) {
    public boolean isConfigured() {
        return accountSid != null && !accountSid.isBlank()
                && authToken != null && !authToken.isBlank()
                && from != null && !from.isBlank();
    }
}
