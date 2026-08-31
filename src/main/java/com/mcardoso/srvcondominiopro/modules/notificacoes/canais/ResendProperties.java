package com.mcardoso.srvcondominiopro.modules.notificacoes.canais;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

// Config do provedor de email (Resend). Sem api-key => canal de email desligado.
@ConfigurationProperties(prefix = "app.resend")
public record ResendProperties(
        String apiKey,

        @DefaultValue("CondominioPro <onboarding@resend.dev>")
        String from,

        @DefaultValue("https://api.resend.com")
        String baseUrl
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
