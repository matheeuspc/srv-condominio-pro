package com.mcardoso.srvcondominiopro.modules.notificacoes.canais;

import com.mcardoso.srvcondominiopro.shared.exceptions.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// Envio de mensagem WhatsApp via Twilio
// (https://www.twilio.com/docs/whatsapp/api). Endpoint de Messages, auth Basic.
@Component
public class WhatsappSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsappSender.class);

    private final RestClient restClient;
    private final TwilioProperties properties;

    public WhatsappSender(RestClient.Builder builder, TwilioProperties properties) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public void enviar(String telefoneDestino, String conteudo) {
        String to = telefoneDestino.startsWith("whatsapp:")
                ? telefoneDestino
                : "whatsapp:" + normalizarE164(telefoneDestino);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", to);
        form.add("From", properties.from());
        form.add("Body", conteudo);

        String basic = Base64.getEncoder().encodeToString(
                (properties.accountSid() + ":" + properties.authToken()).getBytes(StandardCharsets.UTF_8));

        try {
            restClient.post()
                    .uri("/2010-04-01/Accounts/{sid}/Messages.json", properties.accountSid())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            log.warn("Twilio recusou o envio para {}: {} - {}",
                    to, ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new AppException("Twilio recusou o envio: " + ex.getStatusCode().value(), HttpStatus.BAD_GATEWAY);
        } catch (RestClientException ex) {
            log.warn("Erro de comunicação com o Twilio ao enviar para {}", to, ex);
            throw new AppException("Falha de comunicação com o provedor de WhatsApp", HttpStatus.BAD_GATEWAY);
        }
    }

    // Normalização ingênua para E.164: assume Brasil (+55) quando não vier prefixo internacional.
    static String normalizarE164(String telefone) {
        String digitos = telefone.replaceAll("[^0-9+]", "");
        if (digitos.startsWith("+")) {
            return digitos;
        }
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            return "+" + digitos;
        }
        return "+55" + digitos;
    }
}
